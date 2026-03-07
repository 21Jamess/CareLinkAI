package com.example.carelinkai

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
import com.example.carelinkai.ui.DoctorHomeScreen
import com.example.carelinkai.ui.LoginScreen
import com.example.carelinkai.ui.PatientHomeScreen
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
                    val navController    = rememberNavController()
                    val doctorViewModel  : DoctorViewModel  = viewModel()
                    val patientViewModel : PatientViewModel = viewModel()

                    NavHost(
                        navController    = navController,
                        startDestination = Screen.Login.route
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController = navController)
                        }
                        composable(Screen.PatientHome.route) {
                            PatientHomeScreen(viewModel = patientViewModel)
                        }
                        composable(Screen.DoctorHome.route) {
                            DoctorHomeScreen(
                                doctorViewModel  = doctorViewModel,
                                patientViewModel = patientViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
