package com.example.carelinkai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.carelinkai.navigation.Screen

@Composable
fun LoginScreen(navController: NavController) {
    var role by remember { mutableStateOf("Patient") }

    fun proceed() {
        val dest = if (role == "Doctor") Screen.DoctorHome.route else Screen.PatientHome.route
        navController.navigate(dest) {
            // Pop any existing home screen but keep Login so back button returns here
            popUpTo(Screen.Login.route) { inclusive = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CareLink AI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "AI-Driven Patient & Doctor Support",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Role selection
        Text(
            text = "I am a…",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Patient", "Doctor").forEach { r ->
                FilterChip(
                    selected = role == r,
                    onClick = { role = r },
                    label = { Text(r, style = MaterialTheme.typography.bodyLarge) }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedButton(
            onClick = { proceed() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("  or  ", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { proceed() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { proceed() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Create Account")
        }
    }
}
