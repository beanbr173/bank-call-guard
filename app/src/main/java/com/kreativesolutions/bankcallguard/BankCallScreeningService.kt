package com.kreativesolutions.bankcallguard

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.kreativesolutions.bankcallguard.alert.AlertNotificationHelper
import com.kreativesolutions.bankcallguard.domain.Risk
import com.kreativesolutions.bankcallguard.domain.ScamDetectionEngine
import com.kreativesolutions.bankcallguard.history.AlertHistoryStore
import com.kreativesolutions.bankcallguard.prefs.HighBlockMode

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
            enabledBankIds = prefs.enabledBankIds,
            customNumbers = prefs.customNumbers
        )

        if (assessment.risk == Risk.HIGH || assessment.risk == Risk.CAUTION) {
            val playAlarm = assessment.risk == Risk.HIGH && prefs.useScamAlarmRingtone
            var actionTaken = "alerted"

            if (assessment.risk == Risk.HIGH) {
                when (prefs.highBlockMode) {
                    HighBlockMode.OFF -> Unit
                    HighBlockMode.SILENCE -> {
                        responseBuilder.setSilenceCall(true)
                        actionTaken = "silenced"
                    }
                    HighBlockMode.REJECT -> {
                        responseBuilder.setDisallowCall(true)
                        responseBuilder.setRejectCall(true)
                        responseBuilder.setSkipNotification(true)
                        actionTaken = "rejected"
                    }
                }
            }

            if (playAlarm && prefs.highBlockMode != HighBlockMode.REJECT) {
                responseBuilder.setSilenceCall(true)
            } else if (playAlarm && prefs.highBlockMode == HighBlockMode.REJECT) {
                // Reject already blocks the call; alarm still helps via overlay/notification.
            }

            OverlayLauncher.show(applicationContext, assessment, playAlarm = playAlarm)
            AlertNotificationHelper.notifyAlert(applicationContext, assessment, playAlarm)
            app.alertHistoryStore.addBlocking(
                AlertHistoryStore.create(
                    risk = assessment.risk,
                    bankName = assessment.bankName,
                    callerNumber = assessment.callerNumber,
                    callerDisplayName = assessment.callerDisplayName,
                    message = assessment.userMessage,
                    actionTaken = actionTaken
                )
            )
            Log.i(TAG, "Alert shown ($actionTaken): ${assessment.userMessage} for $phoneNumber")
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    companion object {
        private const val TAG = "BankCallScreening"
    }
}
