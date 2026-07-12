package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.models.UserRole
import com.example.ui.CalendarScreen
import com.example.ui.LoginScreen
import com.example.ui.StaffManagementScreen
import kotlinx.serialization.Serializable

@Serializable object LoginRoute
@Serializable object AdminDashboardRoute
@Serializable object ManagerDashboardRoute
@Serializable object ReceptionDashboardRoute
@Serializable object StaffManagementRoute
@Serializable object CalendarRoute
@Serializable data class PaymentRoute(val bookingId: String)
@Serializable object FinancialReportRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // Simple state to hold current user info for navigation
    var currentUserId by remember { mutableStateOf("") }
    var currentUserRole by remember { mutableStateOf("") }

    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = { user ->
                    currentUserId = user.id
                    currentUserRole = user.role.name
                    val destination: Any = when (user.role) {
                        UserRole.Admin -> AdminDashboardRoute
                        UserRole.Manager -> ManagerDashboardRoute
                        UserRole.Receptionist -> ReceptionDashboardRoute
                        UserRole.Accountant -> ManagerDashboardRoute
                    }
                    com.example.repository.Repositories.presence.setUserOnline(user.id)
                    navController.navigate(destination) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<AdminDashboardRoute> {
            com.example.ui.DashboardScreen(
                onNavigateToStaff = { navController.navigate(StaffManagementRoute) },
                onNavigateToCalendar = { navController.navigate(CalendarRoute) },
                onNavigateToReports = { navController.navigate(FinancialReportRoute) },
                onLogout = {
                    navController.navigate(LoginRoute) { popUpTo(0) }
                }
            )
        }

        composable<ManagerDashboardRoute> {
            com.example.ui.DashboardScreen(
                onNavigateToStaff = { navController.navigate(StaffManagementRoute) }, // Might restrict inside
                onNavigateToCalendar = { navController.navigate(CalendarRoute) },
                onNavigateToReports = { navController.navigate(FinancialReportRoute) },
                onLogout = {
                    navController.navigate(LoginRoute) { popUpTo(0) }
                }
            )
        }

        composable<ReceptionDashboardRoute> {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("لوحة تحكم الاستقبال (Receptionist)")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate(CalendarRoute) }) {
                    Text("التقويم والحجوزات")
                }
            }
        }

        composable<StaffManagementRoute> {
            StaffManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<CalendarRoute> {
            CalendarScreen(
                userId = currentUserId,
                userRole = currentUserRole,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable<PaymentRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<PaymentRoute>()
            com.example.ui.PaymentScreen(
                bookingId = args.bookingId,
                userId = currentUserId,
                userRole = currentUserRole,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        
        composable<FinancialReportRoute> {
            com.example.ui.FinancialReportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
