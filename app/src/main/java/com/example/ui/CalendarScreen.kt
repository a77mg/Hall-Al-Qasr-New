@file:Suppress("NewApi", "deprecation")
package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.Booking
import com.example.models.BookingStatus
import com.example.models.EventType
import com.example.models.Shift
import com.example.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    userId: String,
    userRole: String, // To perform autoCleanup if Manager/Admin
    onBack: () -> Unit
) {
    val bookings by viewModel.bookings.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    // Auto cleanup trigger
    LaunchedEffect(Unit) {
        if (userRole == "Admin" || userRole == "Manager") {
            viewModel.performAutoCleanup()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.CreateDocument("text/csv")

    ) { uri ->

        if (uri != null) {

            viewModel.exportBookings(context, uri)

        }

    }



    val importLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.OpenDocument()

    ) { uri ->

        if (uri != null) {

            viewModel.importBookings(context, uri)

        }

    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is com.example.viewmodel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is com.example.viewmodel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("التقويم") },
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
            Row(

                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),

                horizontalArrangement = Arrangement.SpaceEvenly

            ) {

                Button(onClick = {

                    val time = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())

                    exportLauncher.launch("bookings_export_$time.csv")

                }) {

                    Text("تصدير CSV")

                }

                OutlinedButton(onClick = {

                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv"))

                }) {

                    Text("استيراد CSV")

                }

            }
            MonthHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            DaysOfWeekHeader()

            CalendarGrid(
                currentMonth = currentMonth,
                bookings = bookings,
                onDateClick = { date ->
                    selectedDate = date
                    showBookingDialog = true
                }
            )
        }

        if (showBookingDialog && selectedDate != null) {
            val dateBookings = bookings.filter { it.date == selectedDate.toString() }
            
            BookingDialog(
                date = selectedDate!!,
                existingBookings = dateBookings,
                onDismiss = { showBookingDialog = false },
                onBook = { shift, eventType, notes, customerName, customerPhone ->
                    val newBooking = Booking(
                        id = "${selectedDate}_${shift.name}",
                        date = selectedDate.toString(),
                        shift = shift,
                        eventType = eventType,
                        notes = notes,
                        customerId = "NEW_CUSTOMER", // Would normally create customer or pick existing
                        status = BookingStatus.PENDING
                    )
                    viewModel.createBooking(newBooking, userId) {
                        showBookingDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun MonthHeader(currentMonth: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الشهر السابق")
        }
        val monthName = java.time.format.DateTimeFormatter.ofPattern("MMMM", java.util.Locale("ar")).format(currentMonth)
        Text("${monthName} ${currentMonth.year}", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الشهر التالي")
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("ح", "ن", "ث", "ر", "خ", "ج", "س") // Simplified Arabic days (Sun to Sat)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        for (day in daysOfWeek) {
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CalendarGrid(currentMonth: YearMonth, bookings: List<Booking>, onDateClick: (LocalDate) -> Unit) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // Sunday = 0
    
    val totalCells = daysInMonth + firstDayOfMonth
    val cells = (0 until totalCells).toList()

    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxSize()) {
        items(cells) { index ->
            if (index < firstDayOfMonth) {
                Spacer(modifier = Modifier.padding(8.dp))
            } else {
                val day = index - firstDayOfMonth + 1
                val date = currentMonth.atDay(day)
                CalendarDay(date = date, bookings = bookings.filter { it.date == date.toString() }, onClick = { onDateClick(date) })
            }
        }
    }
}

@Composable
fun CalendarDay(date: LocalDate, bookings: List<Booking>, onClick: () -> Unit) {
    val morningBooking = bookings.find { it.shift == Shift.MORNING }
    val eveningBooking = bookings.find { it.shift == Shift.EVENING }
    val fullDayBooking = bookings.find { it.shift == Shift.FULL_DAY }

    val isFullyBooked = fullDayBooking != null || (morningBooking != null && eveningBooking != null)
    
    val bgColor = if (isFullyBooked) {
        Color.Red.copy(alpha = 0.2f)
    } else if (morningBooking != null || eveningBooking != null) {
        Color.Yellow.copy(alpha = 0.3f)
    } else {
        Color.Green.copy(alpha = 0.1f)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(bgColor, shape = MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = date.dayOfMonth.toString())
            if (isFullyBooked) Text("محجوز", fontSize = MaterialTheme.typography.labelSmall.fontSize)
            else if (morningBooking != null || eveningBooking != null) Text("جزئي", fontSize = MaterialTheme.typography.labelSmall.fontSize)
        }
    }
}

@Composable
fun BookingDialog(
    date: LocalDate,
    existingBookings: List<Booking>,
    onDismiss: () -> Unit,
    onBook: (Shift, EventType, String?, String, String) -> Unit
) {
    var selectedShift by remember { mutableStateOf(Shift.EVENING) }
    var eventType by remember { mutableStateOf(EventType.MAQAIL_ZAFA) }
    var notes by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    
    var showError by remember { mutableStateOf(false) }

    val morningTaken = existingBookings.any { it.shift == Shift.MORNING || it.shift == Shift.FULL_DAY }
    val eveningTaken = existingBookings.any { it.shift == Shift.EVENING || it.shift == Shift.FULL_DAY }
    val fullDayTaken = existingBookings.any { it.shift == Shift.FULL_DAY } || (morningTaken && eveningTaken)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حجز جديد: $date") },
        text = {
            Column {
                if (existingBookings.isNotEmpty()) {
                    Text("الحجوزات الحالية:", style = MaterialTheme.typography.titleSmall)
                    existingBookings.forEach { booking ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("الوردية: ${booking.shift.arabicName}")
                                Text("المناسبة: ${booking.eventType.arabicName}")
                                Text("الحالة: ${booking.status.arabicName}")
                                booking.surveillanceLog?.let { log ->
                                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("بيانات المراقبة:", style = MaterialTheme.typography.labelMedium)
                                    Text("من: ${log.startTimeCode} إلى: ${log.endTimeCode}")
                                    Text("الكاميرا: ${log.cameraGroup}")
                                    TextButton(onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("${log.startTimeCode} - ${log.endTimeCode}"))
                                    }) {
                                        Text("نسخ الوقت (Copy Timestamp)")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (fullDayTaken) {
                    Text("هذا اليوم محجوز بالكامل.", color = MaterialTheme.colorScheme.error)
                } else {
                    OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("اسم العميل") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الوردية المتاحة:")
                    if (!morningTaken && !fullDayTaken) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedShift == Shift.MORNING, onClick = { selectedShift = Shift.MORNING })
                            Text("صباحي")
                        }
                    }
                    if (!eveningTaken && !fullDayTaken) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedShift == Shift.EVENING, onClick = { selectedShift = Shift.EVENING })
                            Text("مسائي")
                        }
                    }
                    if (!morningTaken && !eveningTaken && !fullDayTaken) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedShift == Shift.FULL_DAY, onClick = { selectedShift = Shift.FULL_DAY })
                            Text("يوم كامل")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("نوع المناسبة:")
                    EventType.entries.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = eventType == type, onClick = { eventType = type })
                            Text(type.arabicName)
                        }
                    }

                    if (eventType == EventType.OTHER) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("الملاحظات (إلزامي)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (showError) {
                        Text("يرجى ملء كافة الحقول (الملاحظات إلزامية لنوع 'أخرى')", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (!fullDayTaken) {
                Button(onClick = {
                    if (customerName.isBlank() || customerPhone.isBlank()) {
                        showError = true
                    } else if (eventType == EventType.OTHER && notes.isBlank()) {
                        showError = true
                    } else {
                        onBook(selectedShift, eventType, if (notes.isBlank()) null else notes, customerName, customerPhone)
                    }
                }) {
                    Text("تأكيد الحجز")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
