package com.mda.bankcallguard.data

data class BankEntry(
    val bankId: String,
    val displayName: String,
    val numbers: Set<String>,
    val aliases: Set<String>
)

data class BanksCatalog(
    val banks: List<BankEntry>
)
