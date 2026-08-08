package com.kreativesolutions.bankcallguard.data

import android.content.Context
import com.kreativesolutions.bankcallguard.prefs.CustomNumber
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File

class BankNumberRepository(private val context: Context) : BankLookup {
    @Volatile
    private var catalog: BanksCatalog = loadCatalog()

    fun getAllBanks(): List<BankEntry> = catalog.banks

    fun reload() {
        catalog = loadCatalog()
    }

    fun getBanksForScreening(
        enabledBankIds: Set<String>,
        customNumbers: List<CustomNumber>
    ): List<BankEntry> {
        val enabled = getEnabledBanks(enabledBankIds).toMutableList()
        enabled.addAll(customNumbersToBankEntries(customNumbers))
        return enabled
    }

    override fun getEnabledBanks(enabledBankIds: Set<String>): List<BankEntry> {
        if (enabledBankIds.isEmpty()) {
            return emptyList()
        }
        return catalog.banks.filter { it.bankId in enabledBankIds }
    }

    override fun findBankByNumber(number: String?, banks: List<BankEntry>): BankEntry? {
        if (number.isNullOrBlank()) {
            return null
        }
        return banks.firstOrNull { bank -> number in bank.numbers }
    }

    override fun findBankByAlias(displayName: String?, banks: List<BankEntry>): BankEntry? {
        if (displayName.isNullOrBlank()) {
            return null
        }
        val normalizedName = normalizeAliasText(displayName)
        if (normalizedName.isEmpty()) {
            return null
        }
        val tokens = normalizedName.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

        return banks.firstOrNull { bank ->
            bank.aliases.any { alias ->
                matchesAlias(normalizedName, tokens, alias)
            }
        }
    }

    private fun loadCatalog(): BanksCatalog {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        val jsonText = when {
            cacheFile.exists() -> cacheFile.readText()
            else -> context.assets.open("banks.json").bufferedReader().use(BufferedReader::readText)
        }
        return parseCatalog(jsonText)
    }

    companion object {
        const val CACHE_FILE_NAME = "banks_cache.json"
        private const val MIN_ALIAS_LENGTH = 3

        fun parseCatalog(jsonText: String): BanksCatalog {
            val root = JSONObject(jsonText)
            val banksArray = root.getJSONArray("banks")
            val banks = buildList {
                for (index in 0 until banksArray.length()) {
                    val bankObject = banksArray.getJSONObject(index)
                    add(
                        BankEntry(
                            bankId = bankObject.getString("bankId"),
                            displayName = bankObject.getString("displayName"),
                            numbers = bankObject.getJSONArray("numbers")
                                .let { numbersArray ->
                                    buildSet {
                                        for (numberIndex in 0 until numbersArray.length()) {
                                            add(numbersArray.getString(numberIndex))
                                        }
                                    }
                                },
                            aliases = bankObject.getJSONArray("aliases")
                                .let { aliasesArray ->
                                    buildSet {
                                        for (aliasIndex in 0 until aliasesArray.length()) {
                                            val alias = aliasesArray.getString(aliasIndex).lowercase().trim()
                                            if (alias.length >= MIN_ALIAS_LENGTH) {
                                                add(alias)
                                            }
                                        }
                                    }
                                }
                        )
                    )
                }
            }
            require(banks.isNotEmpty()) { "Bank catalog must contain at least one bank" }
            return BanksCatalog(banks)
        }

        fun normalizeAliasText(displayName: String): String {
            return displayName.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun matchesAlias(normalizedName: String, tokens: Set<String>, alias: String): Boolean {
            val normalizedAlias = normalizeAliasText(alias)
            if (normalizedAlias.length < MIN_ALIAS_LENGTH) {
                return false
            }
            if (normalizedName == normalizedAlias) {
                return true
            }
            val aliasTokens = normalizedAlias.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (aliasTokens.size > 1) {
                return normalizedName.contains(normalizedAlias)
            }
            val single = aliasTokens.firstOrNull() ?: return false
            return single in tokens
        }

        fun customNumbersToBankEntries(customNumbers: List<CustomNumber>): List<BankEntry> {
            return customNumbers.map { custom ->
                BankEntry(
                    bankId = "custom_${custom.id}",
                    displayName = custom.label,
                    numbers = setOf(custom.e164),
                    aliases = emptySet()
                )
            }
        }
    }
}
