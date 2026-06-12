package com.mda.bankcallguard.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class BankNumberRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = BankNumberRepository(context)

    @Test
    fun loadsWellsFargoAndBankOfAmerica() {
        val banks = repository.getAllBanks()
        assertEquals(2, banks.size)
        assertNotNull(banks.firstOrNull { it.bankId == "wells_fargo" })
        assertNotNull(banks.firstOrNull { it.bankId == "bank_of_america" })
    }

    @Test
    fun findsBankByNormalizedNumber() {
        val banks = repository.getAllBanks()
        val bank = repository.findBankByNumber("+18008693557", banks)
        assertEquals("wells_fargo", bank?.bankId)
    }

    @Test
    fun findsBankByAlias() {
        val banks = repository.getAllBanks()
        val bank = repository.findBankByAlias("BOFA Fraud Alert", banks)
        assertEquals("bank_of_america", bank?.bankId)
    }
}
