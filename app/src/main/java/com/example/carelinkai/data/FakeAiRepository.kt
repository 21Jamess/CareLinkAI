package com.example.carelinkai.data

// R3 - fake version of the AI so we can demo without needing real API keys
// uses regex to parse goals from text; will be replaced by real OpenAI calls in R8


class FakeAiRepository : AiRepository {

    override suspend fun processText(text: String): AiResult {

        // try to find something like "5000 steps" in the text
        val stepsRegex = Regex("""(\d+)\s*steps""", RegexOption.IGNORE_CASE)
        val match = stepsRegex.find(text)
        val stepsTarget = match?.groupValues?.get(1)?.toIntOrNull() ?: 5000

        // build a realistic looking response
        return AiResult(
            goals = listOf(
                Goal(type = "steps", target = stepsTarget, frequency = "daily")
            ),
            doctorSummary = "Patient should maintain an active lifestyle with a daily goal of " +
                "$stepsTarget steps. Diet should focus on low sodium and high fiber intake. " +
                "Follow-up recommended in 4 weeks to assess progress.",
            patientReminder = "Remember to reach your daily goal of $stepsTarget steps! " +
                "Stay active and keep track of your progress."
        )
    }
}
