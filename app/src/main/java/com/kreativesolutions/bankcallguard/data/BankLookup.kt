package com.kreativesolutions.bankcallguard.data

interface BankLookup {
    fun getEnabledBanks(enabledBankIds: Set<String>): List<BankEntry>

    fun findBankByNumber(number: String?, banks: List<BankEntry>): BankEntry?

    fun findBankByAlias(displayName: String?, banks: List<BankEntry>): BankEntry?
}
