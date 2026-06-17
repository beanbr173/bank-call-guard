package com.kreativesolutions.bankcallguard

import android.content.Context
import android.content.Intent
import com.kreativesolutions.bankcallguard.domain.Assessment
import com.kreativesolutions.bankcallguard.domain.Risk

object OverlayLauncher {
    const val EXTRA_BANK_NAME = "extra_bank_name"
    const val EXTRA_USER_MESSAGE = "extra_user_message"
    const val EXTRA_CALLER_NUMBER = "extra_caller_number"
    const val EXTRA_CALLER_DISPLAY_NAME = "extra_caller_display_name"
    const val EXTRA_RISK = "extra_risk"
    const val EXTRA_PLAY_ALARM = "extra_play_alarm"

    fun show(context: Context, assessment: Assessment, playAlarm: Boolean = false) {
        val intent = Intent(context, IncomingCallOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(EXTRA_BANK_NAME, assessment.bankName)
            putExtra(EXTRA_USER_MESSAGE, assessment.userMessage)
            putExtra(EXTRA_CALLER_NUMBER, assessment.callerNumber)
            putExtra(EXTRA_CALLER_DISPLAY_NAME, assessment.callerDisplayName)
            putExtra(EXTRA_RISK, assessment.risk.name)
            putExtra(EXTRA_PLAY_ALARM, playAlarm)
        }
        context.startActivity(intent)
    }

    fun showSimulated(context: Context, assessment: Assessment, playAlarm: Boolean = false) {
        show(context, assessment, playAlarm)
    }
}
