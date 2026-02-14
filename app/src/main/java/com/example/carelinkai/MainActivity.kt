package com.example.carelinkai

// main entry point - sets up navigation between all our screens
// viewmodels are created here so they dont get destroyed when switching screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carelinkai.navigation.Screen
import com.example.carelinkai.ui.DoctorResultsScreen
import com.example.carelinkai.ui.DoctorSummaryScreen
import com.example.carelinkai.ui.DoctorUploadScreen
import com.example.carelinkai.ui.HomeScreen
import com.example.carelinkai.ui.PatientDashboardScreen
import com.example.carelinkai.ui.theme.CareLinkAITheme
import com.example.carelinkai.viewmodel.DoctorViewModel
import com.example.carelinkai.viewmodel.PatientViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CareLinkAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // these live as long as the activity so data persists between screens
                    val doctorViewModel: DoctorViewModel = viewModel()
                    val patientViewModel: PatientViewModel = viewModel()

                    // all our screens
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController)
                        }
                        composable(Screen.DoctorUpload.route) {
                            DoctorUploadScreen(
                                viewModel = doctorViewModel,
                                navController = navController
                            )
                        }
                        composable(Screen.DoctorResults.route) {
                            DoctorResultsScreen(
                                viewModel = doctorViewModel,
                                navController = navController
                            )
                        }
                        composable(Screen.PatientDashboard.route) {
                            PatientDashboardScreen(
                                viewModel = patientViewModel,
                                navController = navController
                            )
                        }
                        composable(Screen.DoctorSummary.route) {  //R17
                            DoctorSummaryScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
