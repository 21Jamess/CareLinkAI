package com.example.carelinkai.data

// R8 - placeholder for real AI integration (future requirement)
// will need retrofit + OpenAI API key to implement

class RealAiRepository : AiRepository {

    override suspend fun processText(text: String): AiResult {
        // TODO: hook up retrofit and call our actual endpoint here
        throw NotImplementedError(
            "RealAiRepository is a placeholder. Use FakeAiRepository for now."
        )
    }
}
