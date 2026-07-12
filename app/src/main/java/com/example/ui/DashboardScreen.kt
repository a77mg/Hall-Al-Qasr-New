package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.DashboardViewModel
import java.text.DecimalFormat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.example.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToStaff: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToReports: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("لوحة التحكم المركزية") },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث البيانات")
                    }
                    Button(onClick = onLogout) {
                        Text("تسجيل خروج")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("ملخص عمليات اليوم", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (state.todayConfirmedBookingsCount == 0 && state.todayPaymentsReceived == 0L) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("لا توجد عمليات مسجلة لليوم حتى الآن.")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KpiCard(
                        title = "حجوزات اليوم", 
                        value = "${state.todayConfirmedBookingsCount}", 
                        modifier = Modifier.weight(1f).clickable { onNavigateToCalendar() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    KpiCard(
                        title = "مقبوضات اليوم", 
                        value = "${DecimalFormat("#,###").format(state.todayPaymentsReceived)}", 
                        modifier = Modifier.weight(1f).clickable { onNavigateToReports() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    KpiCard(
                        title = "مبالغ معلقة لليوم", 
                        value = "${DecimalFormat("#,###").format(state.todayPendingRemaining)}", 
                        modifier = Modifier.weight(1f).clickable { onNavigateToReports() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("مؤشرات الأداء العامة (KPIs)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KpiCard(title = "إجمالي الإيرادات", value = "${DecimalFormat("#,###").format(state.totalRevenue)} ريال", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                KpiCard(title = "المبالغ المعلقة", value = "${DecimalFormat("#,###").format(state.totalOutstanding)} ريال", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                KpiCard(title = "نسبة الإشغال (الشهر)", value = "${String.format("%.1f", state.occupancyRate)}%", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                KpiCard(title = "حالة الفريق (متصلين)", value = "${state.onlineUsers}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onNavigateToCalendar, modifier = Modifier.fillMaxWidth()) {
                Text("التقويم والحجوزات")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToReports, modifier = Modifier.fillMaxWidth()) {
                Text("التقارير المالية")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToStaff, modifier = Modifier.fillMaxWidth()) {
                Text("إدارة الموظفين")
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
        }
    }
}
