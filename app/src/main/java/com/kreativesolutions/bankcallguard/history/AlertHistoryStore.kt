package com.kreativesolutions.bankcallguard.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kreativesolutions.bankcallguard.domain.Risk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.alertHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bank_call_guard_alert_history"
)

class AlertHistoryStore(private val context: Context) {
    val entries: Flow<List<AlertHistoryEntry>> = context.alertHistoryDataStore.data.map { prefs ->
        parseEntries(prefs[KEY_ENTRIES])
    }

    suspend fun add(entry: AlertHistoryEntry) {
        context.alertHistoryDataStore.edit { prefs ->
            val current = parseEntries(prefs[KEY_ENTRIES]).toMutableList()
            current.add(0, entry)
            while (current.size > MAX_ENTRIES) {
                current.removeAt(current.lastIndex)
            }
            prefs[KEY_ENTRIES] = serializeEntries(current)
        }
    }

    fun addBlocking(entry: AlertHistoryEntry) {
        runBlocking { add(entry) }
    }

    suspend fun clear() {
        context.alertHistoryDataStore.edit { prefs ->
            prefs[KEY_ENTRIES] = "[]"
        }
    }

    companion object {
        private val KEY_ENTRIES = stringPreferencesKey("entries_json")
        private const val MAX_ENTRIES = 50

        fun create(
            risk: Risk,
            bankName: String?,
            callerNumber: String?,
            callerDisplayName: String?,
            message: String?,
            actionTaken: String
        ): AlertHistoryEntry {
            return AlertHistoryEntry(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                risk = risk,
                bankName = bankName,
                callerNumber = callerNumber,
                callerDisplayName = callerDisplayName,
                message = message,
                actionTaken = actionTaken
            )
        }

        private fun parseEntries(raw: String?): List<AlertHistoryEntry> {
            if (raw.isNullOrBlank()) {
                return emptyList()
            }
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val obj = array.getJSONObject(index)
                        add(
                            AlertHistoryEntry(
                                id = obj.getString("id"),
                                timestampMs = obj.getLong("timestampMs"),
                                risk = runCatching {
                                    Risk.valueOf(obj.getString("risk"))
                                }.getOrDefault(Risk.HIGH),
                                bankName = obj.optString("bankName").ifBlank { null },
                                callerNumber = obj.optString("callerNumber").ifBlank { null },
                                callerDisplayName = obj.optString("callerDisplayName").ifBlank { null },
                                message = obj.optString("message").ifBlank { null },
                                actionTaken = obj.optString("actionTaken", "alerted")
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun serializeEntries(entries: List<AlertHistoryEntry>): String {
            val array = JSONArray()
            entries.forEach { entry ->
                array.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("timestampMs", entry.timestampMs)
                        put("risk", entry.risk.name)
                        put("bankName", entry.bankName ?: "")
                        put("callerNumber", entry.callerNumber ?: "")
                        put("callerDisplayName", entry.callerDisplayName ?: "")
                        put("message", entry.message ?: "")
                        put("actionTaken", entry.actionTaken)
                    }
                )
            }
            return array.toString()
        }
    }
}
