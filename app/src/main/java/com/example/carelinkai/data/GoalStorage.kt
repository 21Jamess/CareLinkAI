package com.example.carelinkai.data

// R10 - saves all confirmed goals to SharedPreferences so they survive app restarts
// R11 - saves per-goal progress (steps, water, calories) locally
// R12 - on app launch, goals and progress are restored from here
// R15 - clearGoal() wipes everything when a new PDF is uploaded

import android.content.Context

class GoalStorage(context: Context) {

    private val prefs = context.getSharedPreferences("carelink_prefs", Context.MODE_PRIVATE)

    // R10 - persist all confirmed goals (indexed keys)
    fun saveGoal(result: AiResult) {
        val editor = prefs.edit()
        editor.putInt("goal_count", result.goals.size)
        result.goals.forEachIndexed { i, goal ->
            editor.putString("goal_${i}_type", goal.type)
            editor.putInt("goal_${i}_target", goal.target)
            editor.putString("goal_${i}_frequency", goal.frequency)
        }
        editor.putString("doctor_summary", result.doctorSummary)
        editor.putString("patient_reminder", result.patientReminder)
        editor.apply()
    }

    // R12 - restore goals on startup; supports legacy single-goal format for backwards compat
    fun loadGoal(): AiResult? {
        val summary = prefs.getString("doctor_summary", "") ?: ""
        val reminder = prefs.getString("patient_reminder", "") ?: ""
        val count = prefs.getInt("goal_count", 0)

        if (count > 0) {
            val goals = (0 until count).mapNotNull { i ->
                val type = prefs.getString("goal_${i}_type", null) ?: return@mapNotNull null
                val target = prefs.getInt("goal_${i}_target", 0)
                val frequency = prefs.getString("goal_${i}_frequency", "daily") ?: "daily"
                Goal(type, target, frequency)
            }
            if (goals.isEmpty()) return null
            return AiResult(goals = goals, doctorSummary = summary, patientReminder = reminder)
        }

        // Legacy single-goal fallback (pre-multi-goal saves)
        val type = prefs.getString("goal_type", null) ?: return null
        val target = prefs.getInt("goal_target", 5000)
        val frequency = prefs.getString("goal_frequency", "daily") ?: "daily"
        return AiResult(
            goals = listOf(Goal(type, target, frequency)),
            doctorSummary = summary,
            patientReminder = reminder
        )
    }

    // R15 - called when doctor uploads a new PDF; wipes all old goals and progress
    fun clearGoal() {
        val count = prefs.getInt("goal_count", 1)
        val editor = prefs.edit()
        for (i in 0 until count) {
            editor.remove("goal_${i}_type")
            editor.remove("goal_${i}_target")
            editor.remove("goal_${i}_frequency")
        }
        editor.remove("goal_count")
        // Legacy single-goal keys
        editor.remove("goal_type")
        editor.remove("goal_target")
        editor.remove("goal_frequency")
        editor.remove("doctor_summary")
        editor.remove("patient_reminder")
        // Per-goal today progress
        listOf("steps", "water", "calories").forEach { editor.remove("progress_$it") }
        editor.remove("daily_steps") // legacy
        // Weekly history
        listOf("steps", "water", "calories").forEach { type ->
            (0..6).forEach { i -> editor.remove("week_${type}_$i") }
        }
        editor.apply()
    }

    // R11 - save how much progress the patient has logged for a specific goal type
    fun saveProgress(type: String, value: Float) {
        prefs.edit().putFloat("progress_$type", value).apply()
    }

    // R12 - restore progress for a goal type on startup
    fun loadProgress(type: String): Float = prefs.getFloat("progress_$type", 0f)

    // Save today's value into this week's slot (0=Mon … 6=Sun)
    fun saveTodayProgress(type: String, value: Float) {
        val dayIndex = java.time.LocalDate.now().dayOfWeek.value - 1
        prefs.edit().putFloat("week_${type}_$dayIndex", value).apply()
    }

    // Load 7-day history for a goal type; null means no data was logged that day
    fun loadWeekHistory(type: String): List<Float?> = (0..6).map { i ->
        val v = prefs.getFloat("week_${type}_$i", -1f)
        if (v < 0f) null else v
    }
}
