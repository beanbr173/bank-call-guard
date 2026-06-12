package com.mda.bankcallguard.data

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader

class BankNumberRepository(context: Context) : BankLookup {
    private val catalog: BanksCatalog by lazy { loadCatalog(context) }

    fun getAllBanks(): List<BankEntry> = catalog.banks

    override fun getEnabledBanks(enabledBankIds: Set<String>): List<BankEntry> {
        if (enabledBankIds.isEmpty()) {
            return catalog.banks
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
        val normalizedName = displayName.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()
        if (normalizedName.isEmpty()) {
            return null
        }

        return banks.firstOrNull { bank ->
            bank.aliases.any { alias ->
                normalizedName.contains(alias) || alias.contains(normalizedName)
            }
        }
    }

    private fun loadCatalog(context: Context): BanksCatalog {
        val jsonText = context.assets.open("banks.json").bufferedReader().use(BufferedReader::readText)
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
                                        add(aliasesArray.getString(aliasIndex).lowercase())
                                    }
                                }
                            }
                    )
                )
            }
        }
        return BanksCatalog(banks)
    }
}
