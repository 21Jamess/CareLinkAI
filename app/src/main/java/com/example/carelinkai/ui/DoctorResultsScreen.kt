package com.example.carelinkai.ui

// R6 - doctor reviews AI-suggested goals in a clean, no-scroll layout
// R9 - tap any goal row to open an edit dialog before confirming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.carelinkai.navigation.Screen
import com.example.carelinkai.viewmodel.DoctorViewModel
import com.example.carelinkai.viewmodel.EditableGoal
import com.example.carelinkai.viewmodel.goalLabel

@Composable
fun DoctorResultsScreen(
    viewModel: DoctorViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    // Which goal index is open for editing (null = dialog closed)
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Edit dialog — focused, one goal at a time
    editingIndex?.let { idx ->
        uiState.editableGoals.getOrNull(idx)?.let { eg ->
            EditGoalDialog(
                editableGoal = eg,
                onTargetChange = { viewModel.updateEditableGoalTarget(idx, it) },
                onFrequencyChange = { viewModel.updateEditableGoalFrequency(idx, it) },
                onDismiss = { editingIndex = null }
            )
        }
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (uiState.goalConfirmed) {
                    Button(
                        onClick = { navController.navigate(Screen.PatientDashboard.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go to Patient Dashboard")
                    }
                } else {
                    Button(
                        onClick = { viewModel.confirmGoal() },
                        enabled = uiState.allTargetsValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Goals")
                    }
                }
            }
        }
    ) { innerPadding ->

        if (uiState.aiResult == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No results yet. Process a document first.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // ── Header ──────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Review Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Confirmed banner ─────────────────────────────────────
                if (uiState.goalConfirmed) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Goals confirmed and sent to patient.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Goals list ───────────────────────────────────────────
                Text(
                    text = "Goals",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    uiState.editableGoals.forEachIndexed { idx, eg ->
                        GoalRow(
                            editableGoal = eg,
                            confirmed = uiState.goalConfirmed,
                            onEdit = { editingIndex = idx }
                        )
                        if (idx < uiState.editableGoals.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Collapsible doctor notes ─────────────────────────────
                CollapsibleNotes(summary = uiState.aiResult!!.doctorSummary)
            }
        }
    }
}

// ── Goal row inside the card ─────────────────────────────────────────────────

@Composable
private fun GoalRow(
    editableGoal: EditableGoal,
    confirmed: Boolean,
    onEdit: () -> Unit
) {
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
        if (!confirmed) {
            TextButton(onClick = onEdit) {
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// ── Edit dialog ──────────────────────────────────────────────────────────────

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

                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("daily", "weekly").forEach { freq ->
                        FilterChip(
                            selected = editableGoal.frequency == freq,
                            onClick = { onFrequencyChange(freq) },
                            label = { Text(freq.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = targetValid) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ── Collapsible doctor notes ─────────────────────────────────────────────────

@Composable
private fun CollapsibleNotes(summary: String) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Doctor Notes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                              else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        if (!expanded) {
            TextButton(onClick = { expanded = true }) {
                Text("Show more", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
