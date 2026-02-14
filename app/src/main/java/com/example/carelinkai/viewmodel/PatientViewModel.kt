package com.example.carelinkai.viewmodel

// R5 - patient can view assigned goals and track progress
// R6 - app provides dynamic reminders based on progress

import androidx.lifecycle.ViewModel
import com.example.carelinkai.data.AppState
import com.example.carelinkai.data.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PatientViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PatientUiState())
    val uiState: StateFlow<PatientUiState> = _uiState

    init {
        refreshFromAppState()
    }

    // R5 - grab the latest goal from AppState
    // called every time the patient screen shows up
    fun refreshFromAppState() {
        val result = AppState.latestAiResult
        val goal = result?.goals?.firstOrNull() ?: Goal("steps", 5000, "daily")
        val reminder = result?.patientReminder
            ?: "Remember to stay active and reach your daily step goal!"
        _uiState.value = _uiState.value.copy(
            goal = goal,
            baseReminder = reminder
        )
    }

    // R5 - update steps when user moves the slider
    fun updateSteps(steps: Float) {
        _uiState.value = _uiState.value.copy(currentSteps = steps)
    }
}

// all the state for the patient dashboard
data class PatientUiState(
    val goal: Goal = Goal("steps", 5000, "daily"),
    val baseReminder: String = "Remember to stay active and reach your daily step goal!",
    val currentSteps: Float = 0f
) {
    // R5 - progress bar percentage (0.0 to 1.0)
    val progress: Float
        get() = (currentSteps / goal.target).coerceIn(0f, 1f)

    // R6 - reminder changes based on whether theyre hitting their goal or not
    val reminderText: String
        get() = if (currentSteps < goal.target) {
            "$baseReminder\n\nYou're ${goal.target - currentSteps.toInt()} steps away from your goal!"
        } else {
            "Great job! You've reached your daily goal of ${goal.target} ${goal.type}!"
        }
}
