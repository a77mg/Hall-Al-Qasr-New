package com.example.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // Only format if it's purely digits (we don't want to format '-' or other strings, but we expect positive numbers here)
        val formattedText = try {
            val number = originalText.toLong()
            DecimalFormat("#,###").format(number)
        } catch (e: NumberFormatException) {
            originalText // Fallback in case of non-numeric input
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= originalText.length) return formattedText.length

                // Count how many commas are before the original offset
                var originalIndex = 0
                var transformedIndex = 0
                var commasCount = 0

                while (originalIndex < offset && transformedIndex < formattedText.length) {
                    if (formattedText[transformedIndex] == ',') {
                        commasCount++
                    } else {
                        originalIndex++
                    }
                    transformedIndex++
                }
                
                return offset + commasCount
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formattedText.length) return originalText.length

                var originalIndex = 0
                var transformedIndex = 0

                while (transformedIndex < offset && originalIndex < originalText.length) {
                    if (formattedText[transformedIndex] != ',') {
                        originalIndex++
                    }
                    transformedIndex++
                }

                return originalIndex
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
