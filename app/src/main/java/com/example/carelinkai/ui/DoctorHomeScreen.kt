package com.example.carelinkai.ui

// Doctor shell: bottom nav with Plan (upload→analyze→review→confirm) and Summary tabs.
// The entire doctor workflow lives in one tab — no navigation required.

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.carelinkai.viewmodel.DoctorViewModel
import com.example.carelinkai.viewmodel.EditableGoal
import com.example.carelinkai.viewmodel.PatientViewModel
import com.example.carelinkai.viewmodel.goalLabel

@Composable
fun DoctorHomeScreen(
    doctorViewModel: DoctorViewModel,
    patientViewModel: PatientViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon     = { Icon(Icons.Filled.Home, contentDescription = "Plan") },
                    label    = { Text("Plan") },
                    selected  = selectedTab == 0,
                    onClick   = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Filled.Person, contentDescription = "Summary") },
                    label    = { Text("Summary") },
                    selected  = selectedTab == 1,
                    onClick   = { selectedTab = 1; patientViewModel.loadWeeklyHistory() }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0    -> PlanTab(doctorViewModel, onGoToSummary = { selectedTab = 1 })
                else -> WeeklySummaryContent(patientViewModel)
            }
        }
    }
}

// ── Plan tab — entire doctor workflow in one place ────────────────────────────

@Composable
private fun PlanTab(viewModel: DoctorViewModel, onGoToSummary: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Auto-process once text is extracted
    LaunchedEffect(uiState.extractedText) {
        if (uiState.extractedText != null && uiState.aiResult == null && !uiState.isLoading) {
            viewModel.processWithAi()
        }
    }

    // PDF picker — on pick, extract text immediately (AI auto-chains via LaunchedEffect)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                c.moveToFirst(); c.getString(idx)
            } ?: "document.pdf"
            viewModel.onPdfSelected(uri, name)
            viewModel.extractText()
        }
    }

    // Edit dialog (null = closed)
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    editingIndex?.let { idx ->
        uiState.editableGoals.getOrNull(idx)?.let { eg ->
            EditGoalDialog(
                editableGoal     = eg,
                onTargetChange   = { viewModel.updateEditableGoalTarget(idx, it) },
                onFrequencyChange = { viewModel.updateEditableGoalFrequency(idx, it) },
                onDismiss        = { editingIndex = null }
            )
        }
    }

    // Render state machine
    when {
        uiState.goalConfirmed              -> SuccessContent(onGoToSummary, onReset = { viewModel.reset() })
        uiState.isLoading                  -> LoadingContent()
        uiState.aiResult != null           -> ReviewContent(uiState.editableGoals, uiState.allTargetsValid,
                                                 onEdit = { editingIndex = it },
                                                 doctorSummary = uiState.aiResult?.doctorSummary ?: "",
                                                 onConfirm = { viewModel.confirmGoal() })
        uiState.selectedPdfUri == null     -> UploadContent(onUpload = { launcher.launch(arrayOf("application/pdf")) })
        else                               -> LoadingContent() // extracting
    }
}

// ── State: no PDF yet ─────────────────────────────────────────────────────────

@Composable
private fun UploadContent(onUpload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📋", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Upload a Care Plan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select a PDF and CareLinkAI will extract goals automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUpload,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Choose PDF", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── State: loading / analyzing ────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Analyzing care plan…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── State: review + confirm ───────────────────────────────────────────────────

@Composable
private fun ReviewContent(
    editableGoals: List<EditableGoal>,
    allTargetsValid: Boolean,
    onEdit: (Int) -> Unit,
    doctorSummary: String,
    onConfirm: () -> Unit
) {
    var notesExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Review Goals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Goal rows with tap-to-edit
            Card(modifier = Modifier.fillMaxWidth()) {
                editableGoals.forEachIndexed { idx, eg ->
                    GoalReviewRow(editableGoal = eg, onEdit = { onEdit(idx) })
                    if (idx < editableGoals.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Collapsible doctor notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Doctor Notes", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { notesExpanded = !notesExpanded }) {
                    Icon(
                        imageVector = if (notesExpanded) Icons.Filled.KeyboardArrowUp
                                      else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = doctorSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (notesExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Sticky confirm
        Button(
            onClick = onConfirm,
            enabled = allTargetsValid,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Confirm Goals", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun GoalReviewRow(editableGoal: EditableGoal, onEdit: () -> Unit) {
    val label = goalLabel(editableGoal.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = editableGoal.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${editableGoal.target} $label · ${editableGoal.frequency}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onEdit) { Text("Edit", style = MaterialTheme.typography.labelMedium) }
    }
}

// ── State: confirmed ──────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(onGoToSummary: () -> Unit, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Goals Confirmed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The patient's dashboard has been updated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGoToSummary,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("View Patient Progress")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Upload New Plan")
        }
    }
}

// ── Edit goal dialog ──────────────────────────────────────────────────────────

@Composable
private fun EditGoalDialog(
    editableGoal: EditableGoal,
    onTargetChange: (String) -> Unit,
    onFrequencyChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val label = goalLabel(editableGoal.type)
    val targetValid = editableGoal.target.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = editableGoal.type.replaceFirstChar { it.uppercase() } + " Goal",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = editableGoal.target,
                    onValueChange = onTargetChange,
                    label = { Text("Target ($label)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !targetValid && editableGoal.target.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("daily", "weekly").forEach { freq ->
                        FilterChip(
                            selected = editableGoal.frequency == freq,
                            onClick  = { onFrequencyChange(freq) },
                            label    = { Text(freq.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = targetValid) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
