package com.kreativesolutions.bankcallguard.domain

import com.kreativesolutions.bankcallguard.data.BankEntry
import com.kreativesolutions.bankcallguard.data.BankLookup
import com.kreativesolutions.bankcallguard.data.BankNumberRepository
import com.kreativesolutions.bankcallguard.data.PhoneNumberNormalizer
import com.kreativesolutions.bankcallguard.prefs.CustomNumber

class ScamDetectionEngine(
    private val repository: BankLookup
) {
    fun assess(
        rawNumber: String?,
        callerDisplayName: String?,
        verificationStatus: Int,
        enabledBankIds: Set<String> = emptySet(),
        customNumbers: List<CustomNumber> = emptyList()
    ): Assessment {
        val normalizedNumber = PhoneNumberNormalizer.normalize(rawNumber)
        val enabledBanks = if (repository is BankNumberRepository) {
            repository.getBanksForScreening(enabledBankIds, customNumbers)
        } else {
            val base = repository.getEnabledBanks(enabledBankIds).toMutableList()
            base.addAll(BankNumberRepository.customNumbersToBankEntries(customNumbers))
            base
        }
        val matchedBank = repository.findBankByNumber(normalizedNumber, enabledBanks)
        val aliasMatch = repository.findBankByAlias(callerDisplayName, enabledBanks)

        if (matchedBank != null) {
            return assessKnownBankNumber(
                bank = matchedBank,
                normalizedNumber = normalizedNumber,
                callerDisplayName = callerDisplayName,
                verificationStatus = verificationStatus
            )
        }

        if (aliasMatch != null) {
            return assessAliasMatch(
                bank = aliasMatch,
                normalizedNumber = normalizedNumber,
                callerDisplayName = callerDisplayName,
                verificationStatus = verificationStatus
            )
        }

        return Assessment(
            bankId = null,
            bankName = null,
            risk = Risk.NONE,
            userMessage = null,
            callerNumber = normalizedNumber,
            callerDisplayName = callerDisplayName
        )
    }

    private fun assessKnownBankNumber(
        bank: BankEntry,
        normalizedNumber: String?,
        callerDisplayName: String?,
        verificationStatus: Int
    ): Assessment {
        val risk = when (verificationStatus) {
            VERIFICATION_STATUS_FAILED -> Risk.HIGH
            VERIFICATION_STATUS_NOT_VERIFIED -> Risk.CAUTION
            VERIFICATION_STATUS_PASSED -> Risk.NONE
            else -> Risk.CAUTION
        }

        val message = when (risk) {
            Risk.HIGH -> "Scam posing as ${bank.displayName}"
            Risk.CAUTION -> "Unverified caller claiming to be ${bank.displayName}"
            Risk.NONE -> null
        }

        return Assessment(
            bankId = bank.bankId,
            bankName = bank.displayName,
            risk = risk,
            userMessage = message,
            callerNumber = normalizedNumber,
            callerDisplayName = callerDisplayName
        )
    }

    private fun assessAliasMatch(
        bank: BankEntry,
        normalizedNumber: String?,
        callerDisplayName: String?,
        verificationStatus: Int
    ): Assessment {
        val risk = when (verificationStatus) {
            VERIFICATION_STATUS_PASSED -> Risk.CAUTION
            else -> Risk.HIGH
        }
        val message = when (risk) {
            Risk.CAUTION ->
                "Unverified name match — caller ID says ${bank.displayName} but number is not official"
            else ->
                "Possible scam — caller ID says ${bank.displayName} but number is not official"
        }

        return Assessment(
            bankId = bank.bankId,
            bankName = bank.displayName,
            risk = risk,
            userMessage = message,
            callerNumber = normalizedNumber,
            callerDisplayName = callerDisplayName
        )
    }

    companion object {
        const val VERIFICATION_STATUS_NOT_VERIFIED = 0
        const val VERIFICATION_STATUS_PASSED = 1
        const val VERIFICATION_STATUS_FAILED = 2
    }
}
