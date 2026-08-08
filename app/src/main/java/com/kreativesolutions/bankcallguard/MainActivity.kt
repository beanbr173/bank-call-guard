package com.kreativesolutions.bankcallguard

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kreativesolutions.bankcallguard.data.BankCatalogUpdater
import com.kreativesolutions.bankcallguard.data.PhoneNumberNormalizer
import com.kreativesolutions.bankcallguard.domain.ScamDetectionEngine
import com.kreativesolutions.bankcallguard.history.AlertHistoryEntry
import com.kreativesolutions.bankcallguard.prefs.HighBlockMode
import com.kreativesolutions.bankcallguard.prefs.UserPreferences
import com.kreativesolutions.bankcallguard.ui.theme.BankCallGuardTheme
import java.text.DateFormat
import java.util.Date
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BankCallGuardApp
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val alertsEnabled by app.userPreferences.alertsEnabled.collectAsState(initial = true)
    val highBlockMode by app.userPreferences.highBlockMode.collectAsState(initial = HighBlockMode.OFF)
    val useScamAlarmRingtone by app.userPreferences.useScamAlarmRingtone.collectAsState(initial = true)
    val darkTheme by app.userPreferences.darkTheme.collectAsState(initial = false)
    val enabledBankIds by app.userPreferences.enabledBankIds.collectAsState(
        initial = UserPreferences.DEFAULT_BANK_IDS
    )
    val customNumbers by app.userPreferences.customNumbers.collectAsState(initial = emptyList())
    val bankCatalogLastUpdatedMs by app.userPreferences.bankCatalogLastUpdatedMs.collectAsState(
        initial = null
    )
    val alertHistory by app.alertHistoryStore.entries.collectAsState(initial = emptyList())

    var callScreeningEnabled by remember { mutableStateOf(isCallScreeningRoleHeld(context)) }
    var batteryExemptionEnabled by remember { mutableStateOf(isBatteryExemptionGranted(context)) }
    var catalogBanks by remember { mutableStateOf(app.repository.getAllBanks()) }
    var refreshStatus by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var customLabel by remember { mutableStateOf("") }
    var customNumberInput by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf<String?>(null) }
    var simulateExpanded by remember { mutableStateOf(false) }

    val enabledBanksForSimulate = remember(catalogBanks, enabledBankIds, customNumbers) {
        app.repository.getBanksForScreening(enabledBankIds, customNumbers)
    }
    var selectedSimulateBankId by remember {
        mutableStateOf(enabledBanksForSimulate.firstOrNull()?.bankId.orEmpty())
    }
    LaunchedEffect(enabledBanksForSimulate.map { it.bankId }) {
        if (enabledBanksForSimulate.none { it.bankId == selectedSimulateBankId }) {
            selectedSimulateBankId = enabledBanksForSimulate.firstOrNull()?.bankId.orEmpty()
        }
    }
    val selectedSimulateBank = enabledBanksForSimulate.firstOrNull { it.bankId == selectedSimulateBankId }

    val appVersionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }
    val dateFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* preference already updated */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
                        scope.launch {
                            app.userPreferences.setAlertsEnabled(enabled)
                            if (enabled) {
                                ensureNotificationPermission()
                            }
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.high_block_mode_title),
                    fontWeight = FontWeight.SemiBold
                )
                HighBlockModeOption(
                    label = stringResource(R.string.high_block_mode_off),
                    selected = highBlockMode == HighBlockMode.OFF,
                    onClick = { scope.launch { app.userPreferences.setHighBlockMode(HighBlockMode.OFF) } }
                )
                HighBlockModeOption(
                    label = stringResource(R.string.high_block_mode_silence),
                    selected = highBlockMode == HighBlockMode.SILENCE,
                    onClick = {
                        scope.launch { app.userPreferences.setHighBlockMode(HighBlockMode.SILENCE) }
                    }
                )
                HighBlockModeOption(
                    label = stringResource(R.string.high_block_mode_reject),
                    selected = highBlockMode == HighBlockMode.REJECT,
                    onClick = {
                        scope.launch { app.userPreferences.setHighBlockMode(HighBlockMode.REJECT) }
                    }
                )
                Text(
                    text = stringResource(R.string.high_block_mode_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                app.userPreferences.setEnabledBankIds(
                                    catalogBanks.map { it.bankId }.toSet()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.enable_all_banks))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch { app.userPreferences.setEnabledBankIds(emptySet()) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.disable_all_banks))
                    }
                }
                catalogBanks.forEach { bank ->
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
                    text = stringResource(R.string.custom_numbers_title),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.custom_numbers_body),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_number_label_hint)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = customNumberInput,
                    onValueChange = { customNumberInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_number_value_hint)) },
                    singleLine = true
                )
                if (customError != null) {
                    Text(
                        text = customError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        val normalized = PhoneNumberNormalizer.normalize(customNumberInput)
                        if (normalized.isNullOrBlank()) {
                            customError = context.getString(R.string.custom_number_invalid)
                        } else {
                            customError = null
                            scope.launch {
                                app.userPreferences.addCustomNumber(customLabel, normalized)
                                customLabel = ""
                                customNumberInput = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.add_custom_number))
                }
                customNumbers.forEach { number ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(number.label, fontWeight = FontWeight.Medium)
                            Text(
                                number.e164,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                scope.launch { app.userPreferences.removeCustomNumber(number.id) }
                            }
                        ) {
                            Text(stringResource(R.string.remove_custom_number))
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.bank_list_title),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (bankCatalogLastUpdatedMs == null) {
                        stringResource(R.string.bank_list_never_updated)
                    } else {
                        stringResource(
                            R.string.bank_list_updated,
                            dateFormat.format(Date(bankCatalogLastUpdatedMs!!))
                        )
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (refreshStatus != null) {
                    Text(
                        text = refreshStatus!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (refreshing) return@OutlinedButton
                        refreshing = true
                        refreshStatus = null
                        scope.launch {
                            when (val result = app.bankCatalogUpdater.refresh()) {
                                is BankCatalogUpdater.Result.Success -> {
                                    app.repository.reload()
                                    catalogBanks = app.repository.getAllBanks()
                                    val now = System.currentTimeMillis()
                                    app.userPreferences.setBankCatalogLastUpdatedMs(now)
                                    refreshStatus = context.getString(
                                        R.string.bank_list_refresh_ok,
                                        result.bankCount
                                    )
                                }
                                is BankCatalogUpdater.Result.Failure -> {
                                    refreshStatus = context.getString(
                                        R.string.bank_list_refresh_failed,
                                        result.message
                                    )
                                }
                            }
                            refreshing = false
                        }
                    },
                    enabled = !refreshing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.refresh_bank_list))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.alert_history_title),
                    fontWeight = FontWeight.SemiBold
                )
                if (alertHistory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.alert_history_empty),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    alertHistory.take(20).forEach { entry ->
                        AlertHistoryRow(entry = entry, dateFormat = dateFormat)
                    }
                    TextButton(
                        onClick = { scope.launch { app.alertHistoryStore.clear() } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.clear_alert_history))
                    }
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
                Text(
                    text = stringResource(R.string.simulate_section_title),
                    fontWeight = FontWeight.SemiBold
                )
                if (enabledBanksForSimulate.isEmpty()) {
                    Text(
                        text = "Enable at least one bank or add a custom number to simulate.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = simulateExpanded,
                        onExpandedChange = { simulateExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSimulateBank?.displayName.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.simulate_bank_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = simulateExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = simulateExpanded,
                            onDismissRequest = { simulateExpanded = false }
                        ) {
                            enabledBanksForSimulate.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank.displayName) },
                                    onClick = {
                                        selectedSimulateBankId = bank.bankId
                                        simulateExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val bank = selectedSimulateBank ?: return@Button
                            val assessment = app.detectionEngine.assess(
                                rawNumber = bank.numbers.firstOrNull(),
                                callerDisplayName = bank.displayName,
                                verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_FAILED,
                                enabledBankIds = enabledBankIds,
                                customNumbers = customNumbers
                            )
                            OverlayLauncher.showSimulated(
                                context,
                                assessment,
                                playAlarm = useScamAlarmRingtone
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedSimulateBank != null
                    ) {
                        Text(stringResource(R.string.simulate_scam_call))
                    }
                    OutlinedButton(
                        onClick = {
                            val bank = selectedSimulateBank ?: return@OutlinedButton
                            val assessment = app.detectionEngine.assess(
                                rawNumber = bank.numbers.firstOrNull(),
                                callerDisplayName = bank.displayName,
                                verificationStatus = ScamDetectionEngine.VERIFICATION_STATUS_NOT_VERIFIED,
                                enabledBankIds = enabledBankIds,
                                customNumbers = customNumbers
                            )
                            OverlayLauncher.showSimulated(context, assessment, playAlarm = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedSimulateBank != null
                    ) {
                        Text(stringResource(R.string.simulate_caution_call))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.hang_up_advice),
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.privacy_policy_url)))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.privacy_policy))
        }
        Text(
            text = stringResource(R.string.app_version, appVersionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HighBlockModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun AlertHistoryRow(entry: AlertHistoryEntry, dateFormat: DateFormat) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = entry.message ?: entry.bankName ?: "Alert",
            fontWeight = FontWeight.Medium
        )
        Text(
            text = dateFormat.format(Date(entry.timestampMs)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val details = listOfNotNull(
            entry.callerNumber,
            entry.risk.name,
            stringResource(R.string.alert_history_action, entry.actionTaken)
        ).joinToString(" · ")
        Text(
            text = details,
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
