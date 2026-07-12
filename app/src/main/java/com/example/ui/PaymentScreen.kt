package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.Payment
import com.example.models.PaymentType
import com.example.utils.NumberToArabicWords
import com.example.utils.ThousandsSeparatorVisualTransformation
import com.example.viewmodel.PaymentViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bookingId: String,
    userId: String,
    userRole: String,
    viewModel: PaymentViewModel = viewModel(),
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var amountStr by remember { mutableStateOf("") }
    var receiptNumber by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(PaymentType.CASH) }
    
    val currentDate = LocalDate.now()
    val isEditable = viewModel.isEditable(currentDate, userRole)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل دفعة مالية") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("عودة") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isEditable) {
                Text(
                    text = "الإقفال المالي يمنع تسجيل الدفعات في هذا التاريخ إلا للمدير العام.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(text = "رقم الحجز: $bookingId", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { newValue ->
                    // Only keep digits
                    val digitsOnly = newValue.filter { it.isDigit() }
                    amountStr = digitsOnly
                },
                label = { Text("المبلغ (بالريال اليمني)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable && !loading
            )

            // Dynamic text to convert number to Arabic words
            val parsedAmount = amountStr.toLongOrNull() ?: 0L
            val arabicText = NumberToArabicWords.convert(parsedAmount)
            Text(
                text = arabicText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = receiptNumber,
                onValueChange = { receiptNumber = it },
                label = { Text("رقم السند / الحوالة") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable && !loading
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("طريقة الدفع:", modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PaymentType.entries.forEach { pt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = paymentType == pt,
                            onClick = { paymentType = pt },
                            enabled = isEditable && !loading
                        )
                        Text(pt.arabicName)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (parsedAmount > 0) {
                            val payment = Payment(
                                bookingId = bookingId,
                                amount = parsedAmount,
                                date = currentDate.toString(),
                                paymentType = paymentType,
                                receiptNumber = receiptNumber
                            )
                            viewModel.processPayment(payment, userId, onSuccess)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditable && parsedAmount > 0
                ) {
                    Text("حفظ الدفعة")
                }
            }
        }
    }
}
