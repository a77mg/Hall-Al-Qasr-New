package com.example.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * اختبارات الوحدة للتأكد من سلامة العمليات ونمذجة البيانات (Data classes).
 */
class BookingValidationTest {

    @Test
    fun `test valid booking creation`() {
        val booking = Booking(
            id = "2024-05-10_EVENING",
            date = "2024-05-10",
            shift = Shift.EVENING,
            eventType = EventType.MAQAIL_ZAFA,
            customerId = "CUST123",
            deposit = 1000L,
            liability = 500L
        )
        
        assertEquals("2024-05-10", booking.date)
        assertEquals(Shift.EVENING, booking.shift)
        assertEquals(EventType.MAQAIL_ZAFA, booking.eventType)
    }

    @Test
    fun `test invalid booking with OTHER event type but no notes throws exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Booking(
                id = "2024-05-10_EVENING",
                date = "2024-05-10",
                shift = Shift.EVENING,
                eventType = EventType.OTHER, // نوع 'أخرى'
                notes = null, // بدون ملاحظات
                customerId = "CUST123"
            )
        }
        
        assertEquals("يجب إدخال ملاحظات عند اختيار مناسبة 'أخرى'", exception.message)
    }

    @Test
    fun `test valid booking with OTHER event type and notes`() {
        val booking = Booking(
            id = "2024-05-10_EVENING",
            date = "2024-05-10",
            shift = Shift.EVENING,
            eventType = EventType.OTHER,
            notes = "مؤتمر صحفي",
            customerId = "CUST123"
        )
        
        assertEquals("مؤتمر صحفي", booking.notes)
    }
}
