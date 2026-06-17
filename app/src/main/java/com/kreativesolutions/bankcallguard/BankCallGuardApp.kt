package com.kreativesolutions.bankcallguard

import android.app.Application
import com.kreativesolutions.bankcallguard.data.BankNumberRepository
import com.kreativesolutions.bankcallguard.domain.ScamDetectionEngine
import com.kreativesolutions.bankcallguard.prefs.UserPreferences

class BankCallGuardApp : Application() {
    lateinit var repository: BankNumberRepository
        private set

    lateinit var detectionEngine: ScamDetectionEngine
        private set

    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = BankNumberRepository(this)
        detectionEngine = ScamDetectionEngine(repository)
        userPreferences = UserPreferences(this)
    }

    companion object {
        lateinit var instance: BankCallGuardApp
            private set
    }
}
