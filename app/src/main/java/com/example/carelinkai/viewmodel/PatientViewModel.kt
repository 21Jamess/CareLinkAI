package com.example.carelinkai.viewmodel

// R7  - patient dashboard shows all goals and logs progress linked to weekly history
// R8  - dynamic reminder updates based on overall goal completion
// R11 - per-goal progress saved locally and tracked across the week
// R12 - goals + progress restored after app restart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.carelinkai.data.AppState
import com.example.carelinkai.data.Goal
import com.example.carelinkai.data.GoalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// One day's logged value for a specific goal
data class DayProgress(
    val label: String,       // "Mon", "Tue", …
    val value: Float?,       // null = nothing logged that day
    val target: Int,
    val isToday: Boolean
) {
    val fraction: Float
        get() = if (value != null && target > 0) (value / target).coerceIn(0f, 1f) else 0f
    val met: Boolean
        get() = value != null && value >= target
}

class PatientViewModel(application: Application) : AndroidViewModel(application) {

    private val goalStorage = GoalStorage(application.applicationContext)

    private val _uiState = MutableStateFlow(PatientUiState())
    val uiState: StateFlow<PatientUiState> = _uiState

    // Weekly data keyed by goal type, each with 7 DayProgress entries
    private val _weeklyData = MutableStateFlow<Map<String, List<DayProgress>>>(emptyMap())
    val weeklyData: StateFlow<Map<String, List<DayProgress>>> = _weeklyData

    init {
        refreshFromAppState()
    }

    fun refreshFromAppState() {
        val result = AppState.latestAiResult ?: goalStorage.loadGoal()
        val goals = result?.goals?.ifEmpty { null } ?: listOf(Goal("steps", 5000, "daily"))
        val reminder = result?.patientReminder
            ?: "Remember to stay active and reach your daily goals!"
        val progress = goals.associate { goal -> goal.type to goalStorage.loadProgress(goal.type) }

        _uiState.value = _uiState.value.copy(
            goals = goals,
            baseReminder = reminder,
            progress = progress
        )
        loadWeeklyHistory()
    }

    // Rebuild weekly history from storage for all current goal types
    fun loadWeeklyHistory() {
        val goals = _uiState.value.goals
        val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1
        _weeklyData.value = goals.associate { goal ->
            goal.type to goalStorage.loadWeekHistory(goal.type).mapIndexed { i, value ->
                DayProgress(
                    label   = DAY_LABELS[i],
                    value   = value,
                    target  = goal.target,
                    isToday = i == todayIndex
                )
            }
        }
    }

    // R7/R11 - update progress and persist to both today's slot and weekly history
    fun updateProgress(type: String, value: Float) {
        goalStorage.saveProgress(type, value)
        goalStorage.saveTodayProgress(type, value)
        _uiState.value = _uiState.value.copy(
            progress = _uiState.value.progress + (type to value)
        )
        // Refresh just this type's weekly row
        val goal = _uiState.value.goals.find { it.type == type } ?: return
        val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1
        val updated = _weeklyData.value.toMutableMap()
        updated[type] = goalStorage.loadWeekHistory(type).mapIndexed { i, v ->
            DayProgress(DAY_LABELS[i], v, goal.target, i == todayIndex)
        }
        _weeklyData.value = updated
    }

    fun adjustProgress(type: String, delta: Float) {
        val current = _uiState.value.progressFor(type)
        updateProgress(type, (current + delta).coerceAtLeast(0f))
    }
}

data class PatientUiState(
    val goals: List<Goal> = listOf(Goal("steps", 5000, "daily")),
    val baseReminder: String = "Remember to stay active and reach your daily goals!",
    val progress: Map<String, Float> = emptyMap()
) {
    fun progressFor(type: String): Float = progress[type] ?: 0f

    fun progressFractionFor(goal: Goal): Float =
        (progressFor(goal.type) / goal.target).coerceIn(0f, 1f)

    val goalsMet: Int
        get() = goals.count { progressFor(it.type) >= it.target.toFloat() }

    val allGoalsMet: Boolean
        get() = goals.isNotEmpty() && goalsMet == goals.size

    val reminderText: String
        get() = if (allGoalsMet) {
            "Great job! You've met all your daily goals today!"
        } else {
            val lines = goals
                .filter { progressFor(it.type) < it.target.toFloat() }
                .map { goal ->
                    val remaining = goal.target - progressFor(goal.type).toInt()
                    "$remaining ${goalLabel(goal.type)} remaining for ${goal.type}"
                }
            "$baseReminder\n\n${lines.joinToString("\n")}"
        }
}

fun goalLabel(type: String) = when (type) {
    "steps"    -> "steps"
    "water"    -> "glasses"
    "calories" -> "cal"
    else       -> type
}
