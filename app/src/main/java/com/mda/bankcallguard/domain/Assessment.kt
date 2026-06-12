package com.mda.bankcallguard.domain

enum class Risk {
    NONE,
    CAUTION,
    HIGH
}

data class Assessment(
    val bankId: String?,
    val bankName: String?,
    val risk: Risk,
    val userMessage: String?,
    val callerNumber: String?,
    val callerDisplayName: String?
)
