package com.bankcallguard.data

object PhoneNumberNormalizer {
    fun normalize(rawNumber: String?): String? {
        if (rawNumber.isNullOrBlank()) {
            return null
        }

        val digitsOnly = rawNumber.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return null
        }

        return when {
            digitsOnly.length == 10 -> "+1$digitsOnly"
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> "+$digitsOnly"
            rawNumber.trim().startsWith("+") -> "+$digitsOnly"
            else -> "+$digitsOnly"
        }
    }
}
