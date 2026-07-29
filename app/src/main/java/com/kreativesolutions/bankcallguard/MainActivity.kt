package com.kreativesolutions.bankcallguard

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kreativesolutions.bankcallguard.domain.ScamDetectionEngine
import com.kreativesolutions.bankcallguard.prefs.UserPreferences
import com.kreativesolutions.bankcallguard.ui.theme.BankCallGuardTheme
import kotlinx.coroutines.launch

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

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BankCallGuardApp
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val alertsEnabled by app.userPreferences.alertsEnabled.collectAsState(initial = true)
    val autoSilenceHighRisk by app.userPreferences.autoSilenceHighRisk.collectAsState(initial = false)
    val useScamAlarmRingtone by app.userPreferences.useScamAlarmRingtone.collectAsState(initial = true)
    val darkTheme by app.userPreferences.darkTheme.collectAsState(initial = false)
    val enabledBankIds by app.userPreferences.enabledBankIds.collectAsState(
        initial = UserPreferences.DEFAULT_BANK_IDS
    )

    var callScreeningEnabled by remember { mutableStateOf(isCallScreeningRoleHeld(context)) }
    var batteryExemptionEnabled by remember { mutableStateOf(isBatteryExemptionGranted(context)) }
    val appVersionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                callScreeningEnabled = isCallScreeningRoleHeld(context)
                batteryExemptionEnabled = isBatteryExemptionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        callScreeningEnabled = isCallScreeningRoleHeld(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodyMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.call_screening_role_title),
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = stringResource(R.string.call_screening_role_body))
                Text(
                    text = if (callScreeningEnabled) {
                        stringResource(R.string.call_screening_enabled)
                    } else {
                        stringResource(R.string.call_screening_disabled)
                    },
                    color = if (callScreeningEnabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Button(
                    onClick = { requestCallScreeningRole(context, roleLauncher) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.enable_call_screening))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceSwitch(
                    label = stringResource(R.string.alerts_enabled),
                    checked = alertsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { app.userPreferences.setAlertsEnabled(enabled) }
                    }
                )
                PreferenceSwitch(
                    label = stringResource(R.string.auto_silence_high_risk),
                    checked = autoSilenceHighRisk,
                    onCheckedChange = { enabled ->
                        scope.launch { app.userPreferences.setAutoSilenceHighRisk(enabled) }
                    }
                )
                PreferenceSwitch(
                    label = stringResource(R.string.use_scam_alarm_ringtone),
                    checked = useScamAlarmRingtone,
                    onCheckedChange = { enabled ->
                        scope.launch { app.userPreferences.setUseScamAlarmRingtone(enabled) }
                    }
                )
                Text(
                    text = stringResource(R.string.use_scam_alarm_ringtone_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PreferenceSwitch(
                    label = stringResource(R.string.dark_mode),
                    checked = darkTheme,
                    onCheckedChange = { enabled ->
                        scope.launch { app.userPreferences.setDarkTheme(enabled) }
                    }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.monitored_banks),
                    fontWeight = FontWeight.SemiBold
                )
                app.repository.getAllBanks().forEach { bank ->
                    BankToggleRow(
                        bankName = bank.displayName,
                        checked = bank.bankId in enabledBankIds,
                        onCheckedChange = { checked ->
                            val updated = enabledBankIds.toMutableSet().apply {
                                if (checked) add(bank.bankId) else remove(bank.bankId)
                            }
                            scope.launch { app.userPreferences.setEnabledBankIds(updated) }
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.battery_optimization),
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = stringResource(R.string.battery_optimization_body))
                Text(
                    text = if (batteryExemptionEnabled) {
                        stringResource(R.string.battery_exemption_enabled)
                    } else {
                        stringResource(R.string.battery_exemption_disabled)
                    },
                    color = if (batteryExemptionEnabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                OutlinedButton(
                    onClick = { manageBatteryExemption(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (batteryExemptionEnabled) {
                                R.string.open_battery_settings_to_revert
                            } else {
                                R.string.request_battery_exemption
                            }
                        )
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val assessment = app.detectionEngine.assess(
                            rawNumber = "+18008693557",
                            callerDisplayName = "Wells Fargo",
                            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_FAILED,
                            enabledBankIds = enabledBankIds
                        )
                        OverlayLauncher.showSimulated(
                            context,
                            assessment,
                            playAlarm = useScamAlarmRingtone
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.simulate_scam_call))
                }
                OutlinedButton(
                    onClick = {
                        val assessment = app.detectionEngine.assess(
                            rawNumber = "+18008693557",
                            callerDisplayName = "Wells Fargo",
                            verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_NOT_VERIFIED,
                            enabledBankIds = enabledBankIds
                        )
                        OverlayLauncher.showSimulated(context, assessment, playAlarm = false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.simulate_caution_call))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.hang_up_advice),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(R.string.app_version, appVersionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PreferenceSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BankToggleRow(
    bankName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = bankName, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun isCallScreeningRoleHeld(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return false
    }
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
    return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
}

private fun requestCallScreeningRole(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return
    }
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return
    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        launcher.launch(intent)
    }
}

private fun isBatteryExemptionGranted(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun manageBatteryExemption(context: android.content.Context) {
    if (isBatteryExemptionGranted(context)) {
        openBatterySettingsToRevert(context)
    } else {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}

private fun openBatterySettingsToRevert(context: android.content.Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (_: Exception) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
    }
}
