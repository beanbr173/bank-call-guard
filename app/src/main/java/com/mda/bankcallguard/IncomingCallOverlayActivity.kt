package com.mda.bankcallguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mda.bankcallguard.alert.ScamAlertSoundPlayer
import com.mda.bankcallguard.domain.Risk
import com.mda.bankcallguard.ui.theme.BankCallGuardTheme

class IncomingCallOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bankName = intent.getStringExtra(OverlayLauncher.EXTRA_BANK_NAME)
        val userMessage = intent.getStringExtra(OverlayLauncher.EXTRA_USER_MESSAGE)
        val callerNumber = intent.getStringExtra(OverlayLauncher.EXTRA_CALLER_NUMBER)
        val callerDisplayName = intent.getStringExtra(OverlayLauncher.EXTRA_CALLER_DISPLAY_NAME)
        val risk = intent.getStringExtra(OverlayLauncher.EXTRA_RISK)?.let { value ->
            runCatching { Risk.valueOf(value) }.getOrDefault(Risk.HIGH)
        } ?: Risk.HIGH
        val playAlarm = intent.getBooleanExtra(OverlayLauncher.EXTRA_PLAY_ALARM, false)

        if (playAlarm && risk == Risk.HIGH) {
            ScamAlertSoundPlayer.start(this)
        }

        setContent {
            BankCallGuardTheme(darkTheme = true) {
                IncomingCallOverlayScreen(
                    bankName = bankName,
                    userMessage = userMessage,
                    callerNumber = callerNumber,
                    callerDisplayName = callerDisplayName,
                    risk = risk,
                    onDismiss = {
                        ScamAlertSoundPlayer.stop()
                        finish()
                    },
                    onAnswerAnyway = {
                        ScamAlertSoundPlayer.stop()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        ScamAlertSoundPlayer.stop()
        super.onDestroy()
    }
}

@Composable
private fun IncomingCallOverlayScreen(
    bankName: String?,
    userMessage: String?,
    callerNumber: String?,
    callerDisplayName: String?,
    risk: Risk,
    onDismiss: () -> Unit,
    onAnswerAnyway: () -> Unit
) {
    val accentColor = when (risk) {
        Risk.HIGH -> Color(0xFFD32F2F)
        Risk.CAUTION -> Color(0xFFF57C00)
        Risk.NONE -> Color(0xFF2E7D32)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.height(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userMessage ?: stringResource(R.string.incoming_call),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (!bankName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bankName,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2430), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!callerDisplayName.isNullOrBlank()) {
                Text(
                    text = "Caller ID: $callerDisplayName",
                    color = Color(0xFFE0E0E0),
                    fontSize = 16.sp
                )
            }
            if (!callerNumber.isNullOrBlank()) {
                Text(
                    text = "Number: $callerNumber",
                    color = Color(0xFFE0E0E0),
                    fontSize = 16.sp
                )
            }
            Text(
                text = stringResource(R.string.hang_up_advice),
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAnswerAnyway,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Icon(Icons.Default.Call, contentDescription = null)
            Text(
                text = stringResource(R.string.answer_anyway),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            Text(
                text = stringResource(R.string.dismiss_warning),
                modifier = Modifier.padding(start = 8.dp),
                color = Color.White
            )
        }
    }
}
