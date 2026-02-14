package com.example.carelinkai.navigation

// R7 - all navigation routes in one place so we dont have magic strings everywhere
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object DoctorUpload : Screen("doctor_upload")
    object DoctorResults : Screen("doctor_results")
    object PatientDashboard : Screen("patient_dashboard")
    object DoctorSummary : Screen("doctor_summary")
}
