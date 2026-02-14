package com.example.carelinkai.data

// R3 - data classes that hold the structured output from AI processing

// one goal from the doctor, like "walk 5000 steps daily"
data class Goal(
    val type: String,       // "steps", "calories", etc
    val target: Int,        // the number they need to hit
    val frequency: String   // "daily" or "weekly"
)

// everything the AI gives us after it reads the pdf text
data class AiResult(
    val goals: List<Goal>,
    val doctorSummary: String,
    val patientReminder: String
)
