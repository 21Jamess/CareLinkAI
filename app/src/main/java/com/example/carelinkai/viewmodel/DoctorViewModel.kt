package com.example.carelinkai.viewmodel

// R1 - doctor can upload a PDF care plan
// R2 - app extracts text from uploaded PDFs
// R3 - AI processes extracted text into structured goals
// R4 - doctor can view AI-generated results and summary

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carelinkai.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// using AndroidViewModel here bc we need context for the pdf extractor
class DoctorViewModel(application: Application) : AndroidViewModel(application) {

    // swap this to RealAiRepository when we implement R8
    private val aiRepository: AiRepository = FakeAiRepository()

    private val _uiState = MutableStateFlow(DoctorUiState())
    val uiState: StateFlow<DoctorUiState> = _uiState

    // R1 - called when user picks a pdf from the file picker
    fun onPdfSelected(uri: Uri, fileName: String) {
        Log.d("CareLinkDemo", "PDF_SELECTED: $fileName")

        _uiState.value = _uiState.value.copy(
            selectedPdfUri = uri,
            selectedPdfName = fileName,
            extractedText = null,
            aiResult = null,
            usingFallback = false,
            error = null
        )
    }

    // R2 - extract text from the pdf (runs on background thread)
    fun extractText() {
        val uri = _uiState.value.selectedPdfUri ?: return
        val context = getApplication<Application>().applicationContext

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    PdfTextExtractor.extract(context, uri)
                }
                Log.d("CareLinkDemo", "TEXT_EXTRACTED: ${result.text.length} chars, fallback=${result.usingFallback}")

                _uiState.value = _uiState.value.copy(
                    extractedText = result.text,
                    usingFallback = result.usingFallback,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("CareLinkDemo", "TEXT_EXTRACTION_FAILED: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to extract text: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // R3 - send the extracted text to our AI repo for processing
    fun processWithAi() {
        val text = _uiState.value.extractedText ?: return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = aiRepository.processText(text)

                Log.d("CareLinkDemo", "AI_RESULT_RECEIVED: ${result.goals.size} goals, target=${result.goals.firstOrNull()?.target}")

                _uiState.value = _uiState.value.copy(
                    aiResult = result,
                    isLoading = false
                )

                // R5 - save to shared state so the patient screen can grab it
                AppState.latestAiResult = result
                Log.d("CareLinkDemo", "GOAL_APPLIED: saved to AppState")
            } catch (e: Exception) {
                Log.e("CareLinkDemo", "AI_PROCESSING_FAILED: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "AI processing failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // R3 - clear old result and run AI again
    fun reprocess() {
        Log.d("CareLinkDemo", "REPROCESS: re-running AI")
        _uiState.value = _uiState.value.copy(aiResult = null)
        processWithAi()
    }
}

// all the ui state in one place
data class DoctorUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val extractedText: String? = null,
    val usingFallback: Boolean = false,
    val aiResult: AiResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
