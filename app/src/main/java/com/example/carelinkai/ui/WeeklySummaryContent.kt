package com.example.carelinkai.ui

// Shared weekly summary content — used by both PatientHome and DoctorHome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelinkai.viewmodel.DayProgress
import com.example.carelinkai.viewmodel.PatientViewModel
import com.example.carelinkai.viewmodel.goalLabel

@Composable
fun WeeklySummaryContent(viewModel: PatientViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.loadWeeklyHistory() }

    val uiState    by viewModel.uiState.collectAsState()
    val weeklyData by viewModel.weeklyData.collectAsState()

    val goalTypes = uiState.goals.map { it.type }
    var selectedTab by remember { mutableIntStateOf(0) }

    val selectedType = goalTypes.getOrNull(selectedTab) ?: return
    val days         = weeklyData[selectedType] ?: emptyList()
    val label        = goalLabel(selectedType)
    val target       = uiState.goals.find { it.type == selectedType }?.target ?: 0

    val loggedDays = days.filter { it.value != null }
    val avgValue   = if (loggedDays.isEmpty()) 0f
                     else loggedDays.sumOf { it.value!!.toDouble() }.toFloat() / loggedDays.size
    val daysMet    = days.count { it.met }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab strip to switch goal types
            if (goalTypes.size > 1) {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                    goalTypes.forEachIndexed { idx, type ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick  = { selectedTab = idx },
                            text = {
                                Text(
                                    text = type.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Day rows
            Card(modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { idx, day ->
                    DayRow(day = day, label = label)
                    if (idx < days.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        // Stats card pinned to bottom
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Avg / day",  "${avgValue.toInt()} $label")
                    StatItem("Days met",   "$daysMet / 7")
                    StatItem("Target",     "$target $label")
                }
                if (loggedDays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            daysMet >= 5 -> "On track — great consistency!"
                            daysMet >= 3 -> "Making progress — keep it up."
                            else         -> "Needs improvement — stay consistent."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DayProgress, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))

        if (day.value == null) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
        } else {
            LinearProgressIndicator(
                progress = { day.fraction },
                modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                color = if (day.met) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${day.value.toInt()} $label",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp)
            )
            Text(
                text = if (day.met) "✓" else "·",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (day.met) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
