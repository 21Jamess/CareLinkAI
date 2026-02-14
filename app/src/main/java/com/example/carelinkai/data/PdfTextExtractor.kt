package com.example.carelinkai.data

// R2 - extracts text from uploaded PDFs using the pdfbox library
// falls back to sample text if the PDF can't be read

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfTextExtractor {

    // holds the text we got and whether we had to use the backup text
    data class ExtractionResult(
        val text: String,
        val usingFallback: Boolean
    )

    fun extract(context: Context, uri: Uri): ExtractionResult {
        return try {
            // pdfbox needs this called once before it works
            PDFBoxResourceLoader.init(context)

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ExtractionResult(FALLBACK_TEXT, true)

            // open the pdf and grab all the text from it
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            inputStream.close()

            // if it came back empty (like a scanned image pdf) use our backup
            if (text.isBlank()) {
                ExtractionResult(FALLBACK_TEXT, true)
            } else {
                ExtractionResult(text, false)
            }
        } catch (e: Exception) {
            // something went wrong, just use the sample text so the demo works
            e.printStackTrace()
            ExtractionResult(FALLBACK_TEXT, true)
        }
    }

    // sample care plan we use when the pdf cant be read
    val FALLBACK_TEXT = """
        Patient Care Plan - Dr. Smith
        Date: January 15, 2024

        Diagnosis: Hypertension, mild. BMI slightly elevated.

        Recommendations:
        - Patient should walk at least 5000 steps daily
        - Low sodium diet (under 2300mg/day)
        - High fiber intake recommended
        - Monitor blood pressure weekly at home
        - Follow-up appointment in 4 weeks

        Notes: Patient is motivated and receptive to lifestyle changes.
    """.trimIndent()
}
