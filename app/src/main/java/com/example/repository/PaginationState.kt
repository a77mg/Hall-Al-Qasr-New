package com.example.repository

/**
 * حالة التصفح والتحميل الخاصة بجلب البيانات (Pagination).
 * تفيد في عرض واجهة الهيكل المؤقت (Skeleton Loading) وخيار إعادة المحاولة (Retry).
 */
sealed class PaginationState<out T> {
    object Idle : PaginationState<Nothing>()
    object Loading : PaginationState<Nothing>() // لتحميل الصفحة الأولى (أو إظهار Skeleton)
    object LoadingNextPage : PaginationState<Nothing>() // لتحميل الصفحات الإضافية
    data class Success<T>(val data: List<T>, val hasMore: Boolean) : PaginationState<T>()
    data class Error(val message: String) : PaginationState<Nothing>() // خطأ في جلب الصفحة الأولى
    data class ErrorNextPage(val message: String) : PaginationState<Nothing>() // خطأ في جلب صفحة إضافية
}
