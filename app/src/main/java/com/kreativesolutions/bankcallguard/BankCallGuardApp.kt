package com.kreativesolutions.bankcallguard

import android.app.Application
import com.kreativesolutions.bankcallguard.alert.AlertNotificationHelper
import com.kreativesolutions.bankcallguard.data.BankCatalogUpdater
import com.kreativesolutions.bankcallguard.data.BankNumberRepository
import com.kreativesolutions.bankcallguard.domain.ScamDetectionEngine
import com.kreativesolutions.bankcallguard.history.AlertHistoryStore
import com.kreativesolutions.bankcallguard.prefs.UserPreferences

class BankCallGuardApp : Application() {
    lateinit var repository: BankNumberRepository
        private set

    lateinit var detectionEngine: ScamDetectionEngine
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var alertHistoryStore: AlertHistoryStore
        private set

    lateinit var bankCatalogUpdater: BankCatalogUpdater
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = BankNumberRepository(this)
        detectionEngine = ScamDetectionEngine(repository)
        userPreferences = UserPreferences(this)
        alertHistoryStore = AlertHistoryStore(this)
        bankCatalogUpdater = BankCatalogUpdater(this)
        AlertNotificationHelper.ensureChannel(this)
    }

    companion object {
        lateinit var instance: BankCallGuardApp
            private set
    }
}
