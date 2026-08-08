package com.kreativesolutions.bankcallguard.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bank_call_guard_prefs")

class UserPreferences(private val context: Context) {
    val alertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ALERTS_ENABLED] ?: true
    }

    val highBlockMode: Flow<HighBlockMode> = context.dataStore.data.map { prefs ->
        resolveHighBlockMode(prefs)
    }

    val useScamAlarmRingtone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_SCAM_ALARM_RINGTONE] ?: true
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    val enabledBankIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_BANK_IDS] ?: DEFAULT_BANK_IDS
    }

    val customNumbers: Flow<List<CustomNumber>> = context.dataStore.data.map { prefs ->
        parseCustomNumbers(prefs[KEY_CUSTOM_NUMBERS])
    }

    val bankCatalogLastUpdatedMs: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_BANK_CATALOG_LAST_UPDATED_MS]
    }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setHighBlockMode(mode: HighBlockMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HIGH_BLOCK_MODE] = mode.toStorage()
            prefs.remove(KEY_AUTO_SILENCE_HIGH_RISK)
        }
    }

    suspend fun setUseScamAlarmRingtone(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_SCAM_ALARM_RINGTONE] = enabled
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = enabled
        }
    }

    suspend fun setEnabledBankIds(bankIds: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLED_BANK_IDS] = bankIds
        }
    }

    suspend fun setCustomNumbers(numbers: List<CustomNumber>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_NUMBERS] = serializeCustomNumbers(numbers)
        }
    }

    suspend fun addCustomNumber(label: String, e164: String): CustomNumber {
        val entry = CustomNumber(
            id = UUID.randomUUID().toString(),
            label = label.trim().ifBlank { "Custom" },
            e164 = e164
        )
        val current = customNumbers.first().toMutableList()
        current.add(entry)
        setCustomNumbers(current)
        return entry
    }

    suspend fun removeCustomNumber(id: String) {
        setCustomNumbers(customNumbers.first().filterNot { it.id == id })
    }

    suspend fun setBankCatalogLastUpdatedMs(timestampMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BANK_CATALOG_LAST_UPDATED_MS] = timestampMs
        }
    }

    fun getSnapshot(): PrefsSnapshot = runBlocking {
        val prefs = context.dataStore.data.first()
        PrefsSnapshot(
            alertsEnabled = prefs[KEY_ALERTS_ENABLED] ?: true,
            highBlockMode = resolveHighBlockMode(prefs),
            useScamAlarmRingtone = prefs[KEY_USE_SCAM_ALARM_RINGTONE] ?: true,
            darkTheme = prefs[KEY_DARK_THEME] ?: false,
            enabledBankIds = prefs[KEY_ENABLED_BANK_IDS] ?: DEFAULT_BANK_IDS,
            customNumbers = parseCustomNumbers(prefs[KEY_CUSTOM_NUMBERS]),
            bankCatalogLastUpdatedMs = prefs[KEY_BANK_CATALOG_LAST_UPDATED_MS]
        )
    }

    data class PrefsSnapshot(
        val alertsEnabled: Boolean,
        val highBlockMode: HighBlockMode,
        val useScamAlarmRingtone: Boolean,
        val darkTheme: Boolean,
        val enabledBankIds: Set<String>,
        val customNumbers: List<CustomNumber>,
        val bankCatalogLastUpdatedMs: Long?
    )

    companion object {
        private val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val KEY_AUTO_SILENCE_HIGH_RISK = booleanPreferencesKey("auto_silence_high_risk")
        private val KEY_HIGH_BLOCK_MODE = stringPreferencesKey("high_block_mode")
        private val KEY_USE_SCAM_ALARM_RINGTONE = booleanPreferencesKey("use_scam_alarm_ringtone")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_ENABLED_BANK_IDS = stringSetPreferencesKey("enabled_bank_ids")
        private val KEY_CUSTOM_NUMBERS = stringPreferencesKey("custom_numbers_json")
        private val KEY_BANK_CATALOG_LAST_UPDATED_MS = longPreferencesKey("bank_catalog_last_updated_ms")

        val DEFAULT_BANK_IDS = setOf(
            "wells_fargo",
            "bank_of_america",
            "first_citizens",
            "usaa",
            "chase",
            "citibank",
            "us_bank",
            "golden_1",
            "east_west_bank"
        )

        private fun resolveHighBlockMode(prefs: Preferences): HighBlockMode {
            val stored = prefs[KEY_HIGH_BLOCK_MODE]
            if (stored != null) {
                return HighBlockMode.fromStorage(stored)
            }
            return if (prefs[KEY_AUTO_SILENCE_HIGH_RISK] == true) {
                HighBlockMode.SILENCE
            } else {
                HighBlockMode.OFF
            }
        }

        private fun parseCustomNumbers(raw: String?): List<CustomNumber> {
            if (raw.isNullOrBlank()) {
                return emptyList()
            }
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val obj = array.getJSONObject(index)
                        add(
                            CustomNumber(
                                id = obj.getString("id"),
                                label = obj.getString("label"),
                                e164 = obj.getString("e164")
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun serializeCustomNumbers(numbers: List<CustomNumber>): String {
            val array = JSONArray()
            numbers.forEach { number ->
                array.put(
                    JSONObject().apply {
                        put("id", number.id)
                        put("label", number.label)
                        put("e164", number.e164)
                    }
                )
            }
            return array.toString()
        }
    }
}
