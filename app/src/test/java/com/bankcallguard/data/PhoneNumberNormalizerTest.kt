package com.bankcallguard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {
    @Test
    fun normalizeTenDigitUsNumber() {
        assertEquals("+18008693557", PhoneNumberNormalizer.normalize("800-869-3557"))
    }

    @Test
    fun normalizeElevenDigitUsNumber() {
        assertEquals("+18008693557", PhoneNumberNormalizer.normalize("1-800-869-3557"))
    }

    @Test
    fun normalizeE164Number() {
        assertEquals("+18004321000", PhoneNumberNormalizer.normalize("+1 800 432 1000"))
    }

    @Test
    fun normalizeBlankReturnsNull() {
        assertNull(PhoneNumberNormalizer.normalize(""))
        assertNull(PhoneNumberNormalizer.normalize(null))
    }
}
