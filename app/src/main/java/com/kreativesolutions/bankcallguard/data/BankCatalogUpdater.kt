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

    sealed class UpdateCheck {
        data class Available(
            val localVersion: String?,
            val remoteVersion: String?,
            val diff: CatalogDiff
        ) : UpdateCheck()

        object UpToDate : UpdateCheck()
        data class Failed(val message: String) : UpdateCheck()
    }

    fun localCatalogJson(): String {
        val cacheFile = File(context.filesDir, BankNumberRepository.CACHE_FILE_NAME)
        return when {
            cacheFile.exists() -> cacheFile.readText()
            else -> context.assets.open("banks.json").bufferedReader().use { it.readText() }
        }
    }

    fun localCatalogVersion(): String? = extractVersion(localCatalogJson())

    fun localCatalogFingerprint(): String = normalizeCatalogJson(localCatalogJson())

    suspend fun checkForUpdate(catalogUrl: String = DEFAULT_CATALOG_URL): UpdateCheck =
        withContext(Dispatchers.IO) {
            runCatching {
                val localBody = localCatalogJson()
                val remoteBody = downloadCatalogBody(catalogUrl)
                val localCatalog = BankNumberRepository.parseCatalog(localBody)
                val remoteCatalog = BankNumberRepository.parseCatalog(remoteBody)
                val localVersion = extractVersion(localBody)
                val remoteVersion = extractVersion(remoteBody)
                val remoteFingerprint = normalizeCatalogJson(remoteBody)
                val localFingerprint = normalizeCatalogJson(localBody)
                val versionChanged = !remoteVersion.isNullOrBlank() &&
                    !localVersion.isNullOrBlank() &&
                    remoteVersion != localVersion
                val contentChanged = remoteFingerprint != localFingerprint
                if (versionChanged || contentChanged) {
                    UpdateCheck.Available(
                        localVersion = localVersion,
                        remoteVersion = remoteVersion,
                        diff = CatalogDiff.between(
                            local = localCatalog,
                            remote = remoteCatalog,
                            localVersion = localVersion,
                            remoteVersion = remoteVersion
                        )
                    )
                } else {
                    UpdateCheck.UpToDate
                }
            }.getOrElse { error ->
                UpdateCheck.Failed(error.message ?: "Failed to check for bank list updates")
            }
        }

    suspend fun refresh(catalogUrl: String = DEFAULT_CATALOG_URL): Result = withContext(Dispatchers.IO) {
        runCatching {
            val body = downloadCatalogBody(catalogUrl)
            val catalog = BankNumberRepository.parseCatalog(body)
            val cacheFile = File(context.filesDir, BankNumberRepository.CACHE_FILE_NAME)
            cacheFile.writeText(body)
            Result.Success(bankCount = catalog.banks.size, version = extractVersion(body))
        }.getOrElse { error ->
            Result.Failure(error.message ?: "Failed to refresh bank list")
        }
    }

    private fun downloadCatalogBody(catalogUrl: String): String {
        val connection = (URL(catalogUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("Server returned HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_CATALOG_URL =
            "https://beanbr173.github.io/privacy-policies/bank-call-guard-banks.json"

        fun extractVersion(jsonText: String): String? {
            return runCatching {
                org.json.JSONObject(jsonText).optString("version").ifBlank { null }
            }.getOrNull()
        }

        fun normalizeCatalogJson(jsonText: String): String {
            return jsonText.replace(Regex("\\s+"), "")
        }
    }
}
