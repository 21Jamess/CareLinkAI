package com.example.carelinkai.data

// R5 - shared in-memory state so the patient screen can read goals set by the doctor
// will be replaced by Room database in R10

object AppState {
    var latestAiResult: AiResult? = null
}
