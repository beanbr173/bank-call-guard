package com.mda.bankcallguard.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bank_call_guard_prefs")

class UserPreferences(private val context: Context) {
    val alertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ALERTS_ENABLED] ?: true
    }

    val autoSilenceHighRisk: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SILENCE_HIGH_RISK] ?: false
    }

    val useScamAlarmRingtone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_SCAM_ALARM_RINGTONE] ?: true
    }

    val enabledBankIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_BANK_IDS] ?: DEFAULT_BANK_IDS
    }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setAutoSilenceHighRisk(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SILENCE_HIGH_RISK] = enabled
        }
    }

    suspend fun setUseScamAlarmRingtone(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_SCAM_ALARM_RINGTONE] = enabled
        }
    }

    suspend fun setEnabledBankIds(bankIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED_BANK_IDS] = bankIds
        }
    }

    fun getSnapshot(): PrefsSnapshot = runBlocking {
        val prefs = context.dataStore.data.first()
        PrefsSnapshot(
            alertsEnabled = prefs[KEY_ALERTS_ENABLED] ?: true,
            autoSilenceHighRisk = prefs[KEY_AUTO_SILENCE_HIGH_RISK] ?: false,
            useScamAlarmRingtone = prefs[KEY_USE_SCAM_ALARM_RINGTONE] ?: true,
            enabledBankIds = prefs[KEY_ENABLED_BANK_IDS] ?: DEFAULT_BANK_IDS
        )
    }

    data class PrefsSnapshot(
        val alertsEnabled: Boolean,
        val autoSilenceHighRisk: Boolean,
        val useScamAlarmRingtone: Boolean,
        val enabledBankIds: Set<String>
    )

    companion object {
        private val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val KEY_AUTO_SILENCE_HIGH_RISK = booleanPreferencesKey("auto_silence_high_risk")
        private val KEY_USE_SCAM_ALARM_RINGTONE = booleanPreferencesKey("use_scam_alarm_ringtone")
        private val KEY_ENABLED_BANK_IDS = stringSetPreferencesKey("enabled_bank_ids")

        val DEFAULT_BANK_IDS = setOf("wells_fargo", "bank_of_america")
    }
}
