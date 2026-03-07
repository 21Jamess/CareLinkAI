package com.example.carelinkai.data

// R3 - fake AI that parses multiple goal types from care plan text
// Extracts steps, water, and calorie targets via regex; falls back to safe defaults

class FakeAiRepository : AiRepository {

    override suspend fun processText(text: String): AiResult {

        val stepsTarget = Regex("""(\d+)\s*steps""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 5000

        val waterTarget =
            Regex("""(\d+)\s*(?:glasses|cups)\s*(?:of\s*)?water""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\s*oz\s*(?:of\s*)?(?:water|fluid)""", RegexOption.IGNORE_CASE)
                    .find(text)?.groupValues?.get(1)?.toIntOrNull()
                ?: 8

        val caloriesTarget =
            Regex("""(\d[\d,]*)\s*(?:calories|kcal|cal)\b""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 2000

        return AiResult(
            goals = listOf(
                Goal(type = "steps",    target = stepsTarget,    frequency = "daily"),
                Goal(type = "water",    target = waterTarget,    frequency = "daily"),
                Goal(type = "calories", target = caloriesTarget, frequency = "daily")
            ),
            doctorSummary = "Patient should aim for $stepsTarget steps, $waterTarget glasses of " +
                "water, and $caloriesTarget calories daily. Diet should focus on low sodium and " +
                "high fiber intake. Follow-up recommended in 4 weeks to assess progress.",
            patientReminder = "Stay on track with your daily goals: $stepsTarget steps, " +
                "$waterTarget glasses of water, and $caloriesTarget calories!"
        )
    }
}
