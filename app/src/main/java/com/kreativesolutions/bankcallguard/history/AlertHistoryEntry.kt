package com.kreativesolutions.bankcallguard.history

import com.kreativesolutions.bankcallguard.domain.Risk

data class AlertHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val risk: Risk,
    val bankName: String?,
    val callerNumber: String?,
    val callerDisplayName: String?,
    val message: String?,
    val actionTaken: String
)
