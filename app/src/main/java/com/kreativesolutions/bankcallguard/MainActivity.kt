package com.kreativesolutions.bankcallguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kreativesolutions.bankcallguard.ui.MainScreen
import com.kreativesolutions.bankcallguard.ui.theme.BankCallGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val app = applicationContext as BankCallGuardApp
            val darkTheme by app.userPreferences.darkTheme.collectAsState(
                initial = app.userPreferences.getSnapshot().darkTheme
            )
            BankCallGuardTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
