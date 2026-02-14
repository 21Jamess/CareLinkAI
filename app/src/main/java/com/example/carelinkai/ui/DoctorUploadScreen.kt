package com.example.carelinkai.ui

// R1 - doctor can upload a PDF care plan
// R2 - app extracts text from uploaded PDFs
// R3 - AI processes extracted text into structured goals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.carelinkai.navigation.Screen
import com.example.carelinkai.viewmodel.DoctorViewModel

@Composable
fun DoctorUploadScreen(
    viewModel: DoctorViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // R1 - opens the android file picker filtered to pdfs only
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // grab the file name to show on screen
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val name = cursor?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                if (idx >= 0) c.getString(idx) else "Unknown file"
            } ?: "Unknown file"
            viewModel.onPdfSelected(it, name)
        }
    }

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
            text = "Doctor Upload",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // R1 - pick a pdf
        Button(
            onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select PDF")
        }

        // show which file they picked
        if (uiState.selectedPdfName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Selected: ${uiState.selectedPdfName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // R2 - extract text button (only shows after picking a file)
        if (uiState.selectedPdfUri != null && uiState.extractedText == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.extractText() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Extract Text")
            }
        }

        // R2 - show the extracted text after its done
        if (uiState.extractedText != null) {
            Spacer(modifier = Modifier.height(16.dp))

            // warning if we had to use fallback sample text
            if (uiState.usingFallback) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Could not extract PDF text. Using sample text for demo.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Extracted Text Preview:",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            // only show first 500 chars so it doesnt take up the whole screen
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.extractedText!!.take(500) +
                        if (uiState.extractedText!!.length > 500) "..." else "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // R3 - send it to the AI
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.processWithAi() },
                enabled = !uiState.isLoading && uiState.aiResult == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Process with AI")
            }
        }

        // show these buttons after AI is done
        if (uiState.aiResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate(Screen.DoctorResults.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Results")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // R3 - let them run AI again if they want
            OutlinedButton(
                onClick = { viewModel.reprocess() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reprocess with AI")
            }
        }

        // show error if something broke
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.error!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
