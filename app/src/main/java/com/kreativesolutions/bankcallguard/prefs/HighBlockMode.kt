package com.kreativesolutions.bankcallguard.prefs

enum class HighBlockMode {
    OFF,
    SILENCE,
    REJECT;

    companion object {
        fun fromStorage(value: String?): HighBlockMode {
            return when (value?.lowercase()) {
                "silence" -> SILENCE
                "reject" -> REJECT
                else -> OFF
            }
        }
    }

    fun toStorage(): String = name.lowercase()
}
