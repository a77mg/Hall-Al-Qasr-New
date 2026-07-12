package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * أنواع المناسبات المتاحة في النظام.
 * يتم استخدامها للتحقق في قاعدة البيانات والواجهات.
 */
@Serializable
enum class EventType(val arabicName: String) {
    @SerialName("عرس رجال مقيل وزفة")
    MAQAIL_ZAFA("عرس رجال مقيل وزفة"),
    @SerialName("عرس رجال سمرة")
    SAMRA("عرس رجال سمرة"),
    @SerialName("عرس نساء كراسي وطاولات")
    WOMEN_CHAIRS("عرس نساء كراسي وطاولات"),
    @SerialName("عرس نساء مجالس")
    WOMEN_MAJALIS("عرس نساء مجالس"),
    @SerialName("عزاء")
    CONDOLENCE("عزاء"),
    @SerialName("أخرى")
    OTHER("أخرى")
}

/**
 * الفترات المتاحة للحجز (ورديات).
 */
@Serializable
enum class Shift(val arabicName: String) {
    @SerialName("Morning")
    MORNING("صباحي"),
    @SerialName("Evening")
    EVENING("مسائي"),
    @SerialName("FullDay")
    FULL_DAY("يوم كامل")
}

/**
 * حالات الحجز المتاحة.
 */
@Serializable
enum class BookingStatus(val arabicName: String) {
    @SerialName("Pending")
    PENDING("قيد الانتظار"),
    @SerialName("Confirmed")
    CONFIRMED("مؤكد"),
    @SerialName("Cancelled")
    CANCELLED("ملغي"),
    @SerialName("Completed")
    COMPLETED("مكتمل")
}

@Serializable
enum class PaymentType(val arabicName: String) {
    @SerialName("CASH")
    CASH("نقداً"),
    @SerialName("BANK_TRANSFER")
    BANK_TRANSFER("تحويل بنكي"),
    @SerialName("CHEQUE")
    CHEQUE("شيك")
}

/**
 * نموذج العميل.
 * يتم تخزين البيانات بشكل آمن ومشفّر في الحالات الحساسة.
 */
@Serializable
data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    // يمكن تشفير هذا الحقل في المستودع (Repository) قبل الحفظ
    val encryptedIdentityNumber: String? = null 
)

@Serializable
data class SurveillanceLog(
    val startTimeCode: String = "",
    val endTimeCode: String = "",
    val cameraGroup: String = ""
)

/**
 * نموذج الحجز، يطابق القواعد الأمنية المحددة في firestore.rules
 */
@Serializable
data class Booking(
    val id: String = "", // يجب أن يكون بتنسيق: YYYY-MM-DD_Shift
    val date: String = "", // التنسيق: YYYY-MM-DD
    val shift: Shift = Shift.EVENING,
    val eventType: EventType = EventType.MAQAIL_ZAFA,
    val notes: String? = null,
    val customerId: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val totalAmount: Long = 0,
    val deposit: Long = 0, // الدفعة المقدمة
    val liability: Long = 0, // المديونية (المتبقي)
    val surveillanceLog: SurveillanceLog? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        if (eventType == EventType.OTHER) {
            require(!notes.isNullOrBlank()) { "يجب إدخال ملاحظات عند اختيار مناسبة 'أخرى'" }
        }
        val expectedId = "${date}_${shift.name}"
        // ملاحظة: قد نود التحقق من ID في الـ Repo لكي لا يتعطل الـ Serializer إن كان الـ ID من البداية فارغ
    }
}

/**
 * نموذج الدفعات المالية المرتبطة بحجز معين.
 */
@Serializable
data class Payment(
    val id: String = "",
    val bookingId: String = "",
    val amount: Long = 0,
    val date: String = "", // التنسيق: YYYY-MM-DD
    val paymentType: PaymentType = PaymentType.CASH,
    val receiptNumber: String = "",
    val recordedBy: String = "" // معرّف المستخدم (Admin/Accountant/Manager)
)

@Serializable
data class AuditLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val actionType: String = "",
    val documentId: String = "",
    val oldValue: String? = null,
    val newValue: String? = null
)
