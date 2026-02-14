package com.example.carelinkai.ui

// R4 - doctor can view AI-generated results and summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.carelinkai.navigation.Screen
import com.example.carelinkai.viewmodel.DoctorViewModel

@Composable
fun DoctorResultsScreen(
    viewModel: DoctorViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiResult = uiState.aiResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Text(
            text = "AI Results",
            style = MaterialTheme.typography.headlineMedium
        )

        // nothing to show if they havent processed anything yet
        if (aiResult == null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("No results yet. Please process a document first.")
            return@Column
        }

        Spacer(modifier = Modifier.height(24.dp))

        // R4 - show structured goals as cards
        Text(
            text = "Structured Goals",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        // loop through each goal and make a card for it
        aiResult.goals.forEach { goal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = goal.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Target: ${goal.target} ${goal.type} ${goal.frequency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // R4 - doctor summary from the AI
        Text(
            text = "Doctor Summary",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = aiResult.doctorSummary,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // R4 - patient reminder preview so the doctor can see what the patient gets
        Text(
            text = "Patient Reminder",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Text(
                text = aiResult.patientReminder,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // go check out what the patient sees
        Button(
            onClick = { navController.navigate(Screen.PatientDashboard.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Patient Dashboard")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
