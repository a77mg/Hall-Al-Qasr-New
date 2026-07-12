package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.models.Booking
import com.example.repository.PaginationState
import com.example.viewmodel.BookingViewModel

/**
 * شاشة مؤقتة تعرض بيانات الحجوزات أو حالة الهيكل (Skeleton Loading).
 * تتضمن خيار إعادة المحاولة (Retry) عند الفشل.
 */
@Composable
fun BookingScreen(viewModel: BookingViewModel = BookingViewModel()) {
    val state by viewModel.paginationState.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("قائمة الحجوزات (مع Pagination)") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is PaginationState.Idle, is PaginationState.Loading -> {
                    // واجهة الهيكل المؤقت (Skeleton Loading)
                    SkeletonLoadingList()
                }
                is PaginationState.Error -> {
                    // شاشة الخطأ وزر إعادة المحاولة للصفحة الأولى
                    ErrorRetryView(message = currentState.message, onRetry = { viewModel.retry() })
                }
                is PaginationState.Success -> {
                    BookingList(
                        bookings = currentState.data,
                        hasMore = currentState.hasMore,
                        onLoadMore = { viewModel.fetchNextPage() }
                    )
                }
                is PaginationState.ErrorNextPage -> {
                    // نفضل عرض القائمة المحملة مسبقاً، مع زر إعادة المحاولة أسفلها،
                    // لكن للتبسيط ولإثبات الحالة في الـ State نعرض رسالة عامة فوق القائمة.
                    Column {
                        Text(
                            text = "فشل تحميل الصفحة التالية: ${currentState.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.retry() }, modifier = Modifier.padding(16.dp)) {
                            Text("إعادة المحاولة")
                        }
                    }
                }
                is PaginationState.LoadingNextPage -> {
                    // واجهة Skeleton مبسطة في نهاية القائمة
                    // يمكن معالجتها داخل BookingList عبر الاحتفاظ بالبيانات السابقة
                    // مؤقتاً نعرض Skeleton كامل لسهولة الإثبات (يفضل عرض القائمة + Skeleton في النهاية).
                    SkeletonLoadingList()
                }
            }
        }
    }
}

@Composable
fun BookingList(bookings: List<Booking>, hasMore: Boolean, onLoadMore: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(bookings) { booking ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "رقم الحجز: ${booking.id}", style = MaterialTheme.typography.titleMedium)
                    Text(text = "التاريخ: ${booking.date}")
                    Text(text = "الوردية: ${booking.shift.arabicName}")
                    Text(text = "النوع: ${booking.eventType.arabicName}")
                    Text(text = "الحالة: ${booking.status.arabicName}", color = Color.Blue)
                }
            }
        }
        
        if (hasMore) {
            item {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun SkeletonLoadingList() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(5) { // عرض 5 عناصر كـ Skeleton
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(100.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.5f))
            ) {
                // محتوى الهيكل المؤقت فارغ كإثبات للتصميم
            }
        }
    }
}

@Composable
fun ErrorRetryView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("إعادة المحاولة (Retry)")
        }
    }
}
