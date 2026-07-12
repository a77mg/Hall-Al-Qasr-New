package com.example.utils

object NumberToArabicWords {
    private val units = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    private val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    private val tens = arrayOf("", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    private val hundreds = arrayOf("", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

    fun convert(number: Long): String {
        if (number == 0L) return "صفر ريال يمني فقط"
        if (number < 0) return "سالب " + convert(-number)

        var n = number
        var result = ""

        if (n >= 1_000_000_000) {
            val billions = (n / 1_000_000_000).toInt()
            result += convertGroup(billions) + " مليار "
            n %= 1_000_000_000
        }

        if (n >= 1_000_000) {
            val millions = (n / 1_000_000).toInt()
            result += convertGroup(millions) + " مليون "
            n %= 1_000_000
        }

        if (n >= 1_000) {
            val thousands = (n / 1_000).toInt()
            result += if (result.isNotEmpty()) "و" else ""
            result += if (thousands == 1) "ألف " else if (thousands == 2) "ألفان " else convertGroup(thousands) + " ألف "
            n %= 1_000
        }

        if (n > 0) {
            result += if (result.isNotEmpty()) "و" else ""
            result += convertGroup(n.toInt())
        }

        return result.trim().replace("  ", " ") + " ريال يمني فقط"
    }

    private fun convertGroup(n: Int): String {
        var num = n
        var res = ""
        
        if (num >= 100) {
            res += hundreds[num / 100]
            num %= 100
            if (num > 0) res += " و"
        }

        if (num in 11..19) {
            res += teens[num - 10]
        } else {
            if (num > 0 && num % 10 > 0) {
                res += units[num % 10]
                if (num >= 20) res += " و"
            }
            if (num >= 20 || num == 10) {
                res += tens[num / 10]
            }
        }
        return res
    }
}
