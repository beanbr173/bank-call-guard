package com.kreativesolutions.bankcallguard.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BankCatalogUpdater(private val context: Context) {
    sealed class Result {
        data class Success(val bankCount: Int, val version: String?) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun refresh(catalogUrl: String = DEFAULT_CATALOG_URL): Result = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(catalogUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    return@runCatching Result.Failure("Server returned HTTP $code")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val catalog = BankNumberRepository.parseCatalog(body)
                val cacheFile = File(context.filesDir, BankNumberRepository.CACHE_FILE_NAME)
                cacheFile.writeText(body)
                val version = runCatching {
                    org.json.JSONObject(body).optString("version").ifBlank { null }
                }.getOrNull()
                Result.Success(bankCount = catalog.banks.size, version = version)
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            Result.Failure(error.message ?: "Failed to refresh bank list")
        }
    }

    companion object {
        const val DEFAULT_CATALOG_URL =
            "https://beanbr173.github.io/privacy-policies/bank-call-guard-banks.json"
    }
}
