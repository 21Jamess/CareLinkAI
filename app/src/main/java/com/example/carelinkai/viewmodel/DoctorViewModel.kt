package com.example.carelinkai.viewmodel

// R1  - doctor can upload a PDF care plan
// R2  - app extracts text from uploaded PDFs
// R3  - fallback sample text if extraction fails
// R4  - doctor can trigger AI processing
// R5  - fake AI returns structured goals (steps, water, calories)
// R6  - doctor can view structured goals + summary
// R9  - doctor can edit AI-generated goals before confirming
// R10 - confirmed goals saved locally via GoalStorage
// R15 - uploading a new PDF resets the old goals

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

// Represents one row in the doctor's edit section before confirming
data class EditableGoal(
    val type: String,
    val target: String,
    val frequency: String
)

class DoctorViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository: AiRepository = FakeAiRepository()
    private val goalStorage = GoalStorage(application.applicationContext)

    private val _uiState = MutableStateFlow(DoctorUiState())
    val uiState: StateFlow<DoctorUiState> = _uiState

    // R1 - called when user picks a pdf from the file picker
    // R15 - also clears any previously saved goals so old data doesn't carry over
    fun onPdfSelected(uri: Uri, fileName: String) {
        Log.d("CareLinkDemo", "PDF_SELECTED: $fileName")

        goalStorage.clearGoal()
        AppState.latestAiResult = null

        _uiState.value = _uiState.value.copy(
            selectedPdfUri = uri,
            selectedPdfName = fileName,
            extractedText = null,
            aiResult = null,
            editableGoals = emptyList(),
            goalConfirmed = false,
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

    // R4/R5 - send extracted text to AI, then pre-fill editable fields for R9
    fun processWithAi() {
        val text = _uiState.value.extractedText ?: return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = aiRepository.processText(text)

                Log.d("CareLinkDemo", "AI_RESULT_RECEIVED: ${result.goals.size} goals")

                // R9 - pre-populate editable fields with AI suggestions
                val editable = result.goals.map { goal ->
                    EditableGoal(
                        type = goal.type,
                        target = goal.target.toString(),
                        frequency = goal.frequency
                    )
                }

                _uiState.value = _uiState.value.copy(
                    aiResult = result,
                    editableGoals = editable,
                    goalConfirmed = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("CareLinkDemo", "AI_PROCESSING_FAILED: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "AI processing failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun reprocess() {
        Log.d("CareLinkDemo", "REPROCESS: re-running AI")
        _uiState.value = _uiState.value.copy(aiResult = null, goalConfirmed = false)
        processWithAi()
    }

    // Clears all state so the doctor can upload a new plan from scratch
    fun reset() {
        goalStorage.clearGoal()
        AppState.latestAiResult = null
        _uiState.value = DoctorUiState()
        Log.d("CareLinkDemo", "RESET: cleared all state")
    }

    // R9 - doctor updates the target for one of the goals
    fun updateEditableGoalTarget(index: Int, value: String) {
        val updated = _uiState.value.editableGoals.toMutableList()
        if (index in updated.indices) {
            updated[index] = updated[index].copy(target = value)
            _uiState.value = _uiState.value.copy(editableGoals = updated)
        }
    }

    // R9 - doctor toggles the frequency for one of the goals
    fun updateEditableGoalFrequency(index: Int, value: String) {
        val updated = _uiState.value.editableGoals.toMutableList()
        if (index in updated.indices) {
            updated[index] = updated[index].copy(frequency = value)
            _uiState.value = _uiState.value.copy(editableGoals = updated)
        }
    }

    // R9 + R10 - doctor confirms all (possibly edited) goals; saves to local storage + AppState
    fun confirmGoal() {
        val aiResult = _uiState.value.aiResult ?: return
        val editable = _uiState.value.editableGoals
        if (editable.isEmpty()) return

        val confirmedGoals = editable.mapNotNull { eg ->
            val target = eg.target.toIntOrNull() ?: return@mapNotNull null
            Goal(type = eg.type, target = target, frequency = eg.frequency)
        }
        if (confirmedGoals.isEmpty()) return

        val confirmedResult = aiResult.copy(goals = confirmedGoals)

        goalStorage.saveGoal(confirmedResult)
        AppState.latestAiResult = confirmedResult

        _uiState.value = _uiState.value.copy(goalConfirmed = true)

        Log.d("CareLinkDemo", "GOAL_CONFIRMED: ${confirmedGoals.size} goals saved")
    }
}

data class DoctorUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val extractedText: String? = null,
    val usingFallback: Boolean = false,
    val aiResult: AiResult? = null,
    // R9 - editable fields the doctor can adjust before confirming
    val editableGoals: List<EditableGoal> = emptyList(),
    val goalConfirmed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // True only when every editable goal has a valid integer target
    val allTargetsValid: Boolean
        get() = editableGoals.isNotEmpty() && editableGoals.all { it.target.toIntOrNull() != null }
}
