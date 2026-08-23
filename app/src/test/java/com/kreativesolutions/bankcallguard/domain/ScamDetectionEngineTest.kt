package com.kreativesolutions.bankcallguard.domain

import com.kreativesolutions.bankcallguard.data.BankEntry
import com.kreativesolutions.bankcallguard.data.BankLookup
import com.kreativesolutions.bankcallguard.data.BankNumberRepository
import com.kreativesolutions.bankcallguard.prefs.CustomNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ScamDetectionEngineTest {
    private lateinit var engine: ScamDetectionEngine

    private val wellsFargo = BankEntry(
        bankId = "wells_fargo",
        displayName = "Wells Fargo",
        numbers = setOf("+18008693557"),
        aliases = setOf("wells fargo", "wellsfargo")
    )

    private val bankOfAmerica = BankEntry(
        bankId = "bank_of_america",
        displayName = "Bank of America",
        numbers = setOf("+18004321000"),
        aliases = setOf("bank of america", "bofa")
    )

    @Before
    fun setUp() {
        engine = ScamDetectionEngine(FakeBankLookup(listOf(wellsFargo, bankOfAmerica)))
    }

    @Test
    fun knownBankNumberWithFailedVerificationIsHighRisk() {
        val assessment = engine.assess(
            rawNumber = "800-869-3557",
            callerDisplayName = "Wells Fargo",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_FAILED,
            enabledBankIds = setOf("wells_fargo", "bank_of_america")
        )

        assertEquals(Risk.HIGH, assessment.risk)
        assertEquals("Scam posing as Wells Fargo", assessment.userMessage)
        assertEquals("wells_fargo", assessment.bankId)
    }

    @Test
    fun knownBankNumberWithNotVerifiedIsCaution() {
        val assessment = engine.assess(
            rawNumber = "+18004321000",
            callerDisplayName = null,
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_NOT_VERIFIED,
            enabledBankIds = setOf("wells_fargo", "bank_of_america")
        )

        assertEquals(Risk.CAUTION, assessment.risk)
        assertEquals("Unverified caller claiming to be Bank of America", assessment.userMessage)
    }

    @Test
    fun knownBankNumberWithPassedVerificationIsNone() {
        val assessment = engine.assess(
            rawNumber = "+18008693557",
            callerDisplayName = "Wells Fargo",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_PASSED,
            enabledBankIds = setOf("wells_fargo", "bank_of_america")
        )

        assertEquals(Risk.NONE, assessment.risk)
        assertNull(assessment.userMessage)
    }

    @Test
    fun aliasMatchWithUnknownNumberIsHighRisk() {
        val assessment = engine.assess(
            rawNumber = "+15551234567",
            callerDisplayName = "Bank of America Fraud",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_NOT_VERIFIED,
            enabledBankIds = setOf("wells_fargo", "bank_of_america")
        )

        assertEquals(Risk.HIGH, assessment.risk)
        assertEquals(
            "Possible scam — caller ID says Bank of America but number is not official",
            assessment.userMessage
        )
    }

    @Test
    fun aliasMatchWithPassedVerificationIsCaution() {
        val assessment = engine.assess(
            rawNumber = "+15551234567",
            callerDisplayName = "Bank of America Fraud",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_PASSED,
            enabledBankIds = setOf("wells_fargo", "bank_of_america")
        )

        assertEquals(Risk.CAUTION, assessment.risk)
    }

    @Test
    fun unrelatedCallIsNone() {
        val assessment = engine.assess(
            rawNumber = "+15559876543",
            callerDisplayName = "Mom",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_NOT_VERIFIED,
            enabledBankIds = setOf("wells_fargo", "bank_of_america")
        )

        assertEquals(Risk.NONE, assessment.risk)
        assertNull(assessment.userMessage)
    }

    @Test
    fun disabledBankIsIgnored() {
        val assessment = engine.assess(
            rawNumber = "+18008693557",
            callerDisplayName = "Wells Fargo",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_FAILED,
            enabledBankIds = setOf("bank_of_america")
        )

        assertEquals(Risk.NONE, assessment.risk)
    }

    @Test
    fun emptyEnabledBankIdsMeansNone() {
        val assessment = engine.assess(
            rawNumber = "+18008693557",
            callerDisplayName = "Wells Fargo",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_FAILED,
            enabledBankIds = emptySet()
        )

        assertEquals(Risk.NONE, assessment.risk)
    }

    @Test
    fun customNumberIsMatched() {
        val assessment = engine.assess(
            rawNumber = "+15550001111",
            callerDisplayName = null,
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_FAILED,
            enabledBankIds = emptySet(),
            customNumbers = listOf(
                CustomNumber(id = "1", label = "My CU", e164 = "+15550001111")
            )
        )

        assertEquals(Risk.HIGH, assessment.risk)
        assertEquals("My CU", assessment.bankName)
        assertEquals("Watch-list number: My CU", assessment.userMessage)
    }

    @Test
    fun customNumberIsHighRiskEvenWhenVerificationPassed() {
        val assessment = engine.assess(
            rawNumber = "555-000-1111",
            callerDisplayName = "Brian",
            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_PASSED,
            enabledBankIds = emptySet(),
            customNumbers = listOf(
                CustomNumber(id = "1", label = "Brian", e164 = "+15550001111")
            )
        )

        assertEquals(Risk.HIGH, assessment.risk)
        assertEquals("Watch-list number: Brian", assessment.userMessage)
        assertEquals("custom_1", assessment.bankId)
    }

    private class FakeBankLookup(
        private val banks: List<BankEntry>
    ) : BankLookup {
        override fun getEnabledBanks(enabledBankIds: Set<String>): List<BankEntry> {
            if (enabledBankIds.isEmpty()) {
                return emptyList()
            }
            return banks.filter { it.bankId in enabledBankIds }
        }

        override fun findBankByNumber(number: String?, banks: List<BankEntry>): BankEntry? {
            if (number.isNullOrBlank()) {
                return null
            }
            return banks.firstOrNull { number in it.numbers }
        }

        override fun findBankByAlias(displayName: String?, banks: List<BankEntry>): BankEntry? {
            if (displayName.isNullOrBlank()) {
                return null
            }
            val normalizedName = BankNumberRepository.normalizeAliasText(displayName)
            val tokens = normalizedName.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
            return banks.firstOrNull { bank ->
                bank.aliases.any { alias ->
                    BankNumberRepository.matchesAlias(normalizedName, tokens, alias)
                }
            }
        }
    }
}
