package com.example.carelinkai.navigation

// all navigation routes in one place so we dont have magic strings everywhere
sealed class Screen(val route: String) {
    object Login         : Screen("login")
    object PatientHome   : Screen("patient_home")
    object DoctorHome    : Screen("doctor_home")
    // Legacy routes kept so old screen files still compile
    object Home          : Screen("home")
    object DoctorUpload  : Screen("doctor_upload")
    object DoctorResults : Screen("doctor_results")
    object PatientDashboard : Screen("patient_dashboard")
    object DoctorSummary : Screen("doctor_summary")
}
