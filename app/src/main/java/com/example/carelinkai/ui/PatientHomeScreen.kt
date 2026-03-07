package com.example.carelinkai.ui

// Patient shell: bottom nav with Today (log goals) and Progress (weekly summary) tabs.
// Replaces PatientDashboardScreen + DoctorSummaryScreen — no back-stack navigation needed.

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.carelinkai.data.Goal
import com.example.carelinkai.viewmodel.PatientUiState
import com.example.carelinkai.viewmodel.PatientViewModel
import com.example.carelinkai.viewmodel.goalLabel

private fun stepFor(goal: Goal) = when (goal.type) {
    "steps"    -> 500f
    "water"    -> 1f
    "calories" -> 100f
    else       -> (goal.target / 10f).coerceAtLeast(1f)
}

@Composable
fun PatientHomeScreen(viewModel: PatientViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Refresh goals whenever Today tab is active so doctor's latest plan is always shown
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.refreshFromAppState()
            1 -> viewModel.loadWeeklyHistory()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon    = { Icon(Icons.Filled.Home, contentDescription = "Today") },
                    label   = { Text("Today") },
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon    = { Icon(Icons.Filled.Favorite, contentDescription = "Progress") },
                    label   = { Text("Progress") },
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0    -> TodayTab(viewModel)
                else -> WeeklySummaryContent(viewModel)
            }
        }
    }
}

// ── Today tab ────────────────────────────────────────────────────────────────

@Composable
private fun TodayTab(viewModel: PatientViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val dateLabel = remember {
        java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column {
            Text(
                text = "Today's Goals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Overall completion strip
            val met   = uiState.goalsMet
            val total = uiState.goals.size
            val allMet = uiState.allGoalsMet
            val avgPct = if (total == 0) 0
                         else (uiState.goals.sumOf { uiState.progressFractionFor(it).toDouble() } / total * 100).toInt()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (allMet) MaterialTheme.colorScheme.tertiaryContainer
                                     else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (allMet) "All goals complete!" else "$met of $total goals complete",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (allMet) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$avgPct%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (allMet) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Goal cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.goals.forEach { goal ->
                GoalCard(
                    goal        = goal,
                    uiState     = uiState,
                    onDecrement = { viewModel.adjustProgress(goal.type, -stepFor(goal)) },
                    onIncrement = { viewModel.adjustProgress(goal.type, +stepFor(goal)) },
                    onSetValue  = { viewModel.updateProgress(goal.type, it) }
                )
            }
        }

        // Tip / reminder (compact — tabs handle nav)
        val tipColor = if (uiState.allGoalsMet) MaterialTheme.colorScheme.tertiaryContainer
                       else MaterialTheme.colorScheme.surfaceVariant
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = tipColor)
        ) {
            Text(
                text = if (uiState.allGoalsMet) "You've hit all your goals today — amazing work!"
                       else uiState.reminderText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.allGoalsMet) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Goal card ─────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goal: Goal,
    uiState: PatientUiState,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onSetValue: (Float) -> Unit
) {
    val current  = uiState.progressFor(goal.type)
    val fraction = uiState.progressFractionFor(goal)
    val label    = goalLabel(goal.type)
    val met      = current >= goal.target.toFloat()
    val pct      = (fraction * 100).toInt()
    val focusManager = LocalFocusManager.current

    var localText by remember { mutableStateOf(current.toInt().toString()) }
    var hasFocus  by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        if (!hasFocus) localText = current.toInt().toString()
    }

    fun commit() {
        val v = localText.toFloatOrNull()
        if (v != null && v >= 0f) onSetValue(v)
        else localText = current.toInt().toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (met) MaterialTheme.colorScheme.tertiaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Row 1: name + pct
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (met) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: progress bar
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: target label + [−] [input] [+]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "/ ${goal.target} $label",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { focusManager.clearFocus(); onDecrement() },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Text("−", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }

                    OutlinedTextField(
                        value = localText,
                        onValueChange = { localText = it.filter { c -> c.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { commit(); focusManager.clearFocus() }
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .width(68.dp)
                            .onFocusChanged { state ->
                                if (!state.isFocused && hasFocus) commit()
                                hasFocus = state.isFocused
                            }
                    )

                    FilledIconButton(
                        onClick = { focusManager.clearFocus(); onIncrement() },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
