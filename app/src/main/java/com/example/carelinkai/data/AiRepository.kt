package com.example.carelinkai.data

// R3 - interface so we can swap between our fake AI and a real one later
// right now we use FakeAiRepository, but when we get API keys we just
// switch to RealAiRepository and nothing else has to change
// (R8 - real AI integration is a future requirement)

interface AiRepository {
    suspend fun processText(text: String): AiResult
}
