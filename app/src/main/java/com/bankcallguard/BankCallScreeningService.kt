package com.bankcallguard

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.bankcallguard.domain.Risk
import com.bankcallguard.domain.ScamDetectionEngine

class BankCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val app = application as BankCallGuardApp
        val prefs = app.userPreferences.getSnapshot()

        val responseBuilder = CallScreeningService.CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)

        if (!prefs.alertsEnabled) {
            respondToCall(callDetails, responseBuilder.build())
            return
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        val displayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callDetails.callerDisplayName
        } else {
            null
        }
        val verificationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callDetails.callerNumberVerificationStatus
        } else {
            ScamDetectionEngine.VERIFICATION_STATUS_NOT_VERIFIED
        }

        val assessment = app.detectionEngine.assess(
            rawNumber = phoneNumber,
            callerDisplayName = displayName,
            verificationStatus = verificationStatus,
            enabledBankIds = prefs.enabledBankIds
        )

        if (assessment.risk == Risk.HIGH || assessment.risk == Risk.CAUTION) {
            val playAlarm = assessment.risk == Risk.HIGH && prefs.useScamAlarmRingtone
            if (playAlarm || (prefs.autoSilenceHighRisk && assessment.risk == Risk.HIGH)) {
                responseBuilder.setSilenceCall(true)
            }
            OverlayLauncher.show(applicationContext, assessment, playAlarm = playAlarm)
            Log.i(TAG, "Alert shown: ${assessment.userMessage} for $phoneNumber")
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    companion object {
        private const val TAG = "BankCallScreening"
    }
}
