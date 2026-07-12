@file:Suppress("NewApi", "deprecation")
package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.Payment
import com.example.viewmodel.PaymentViewModel
import java.text.DecimalFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.viewmodel.UiEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportScreen(
    viewModel: PaymentViewModel = viewModel(),
    onBack: () -> Unit
) {
    val payments by viewModel.payments.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPayments(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPayments(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
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
                title = { Text("التقارير المالية (قراءة فقط)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة")
                    }
                },
                actions = {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    IconButton(onClick = { viewModel.manualRefresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث يدوي")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Total Revenue
            val totalRevenue = payments.sumOf { it.amount }
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي الإيرادات للفترة المحددة", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${DecimalFormat("#,###").format(totalRevenue)} ريال يمني",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val time = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("payments_export_$time.csv")
                }) {
                    Text("تصدير CSV")
                }
                OutlinedButton(onClick = {
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv"))
                }) {
                    Text("استيراد CSV")
                }
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(payments) { payment ->
                    PaymentItemCard(payment)
                }
            }
        }
    }
}

@Composable
fun PaymentItemCard(payment: Payment) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("المبلغ: ${DecimalFormat("#,###").format(payment.amount)} ريال", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50))
                Text("تاريخ الدفع: ${payment.date}")
                Text("نوع الدفع: ${payment.paymentType.arabicName}")
                if (payment.receiptNumber.isNotEmpty()) {
                    Text("رقم السند: ${payment.receiptNumber}", style = MaterialTheme.typography.bodySmall)
                }
                Text("رقم الحجز: ${payment.bookingId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
