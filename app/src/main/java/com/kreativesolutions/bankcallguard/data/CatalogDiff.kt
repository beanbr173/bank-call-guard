package com.kreativesolutions.bankcallguard.data

data class CatalogDiff(
    val localVersion: String?,
    val remoteVersion: String?,
    val addedBanks: List<BankEntry>,
    val removedBanks: List<BankEntry>,
    val banksWithAddedNumbers: List<NumberChange>,
    val banksWithRemovedNumbers: List<NumberChange>
) {
    data class NumberChange(
        val bankId: String,
        val bankName: String,
        val numbers: List<String>
    )

    val hasChanges: Boolean
        get() = addedBanks.isNotEmpty() ||
            removedBanks.isNotEmpty() ||
            banksWithAddedNumbers.isNotEmpty() ||
            banksWithRemovedNumbers.isNotEmpty()

    companion object {
        fun between(local: BanksCatalog, remote: BanksCatalog, localVersion: String?, remoteVersion: String?): CatalogDiff {
            val localById = local.banks.associateBy { it.bankId }
            val remoteById = remote.banks.associateBy { it.bankId }

            val addedBanks = remote.banks.filter { it.bankId !in localById }
            val removedBanks = local.banks.filter { it.bankId !in remoteById }

            val banksWithAddedNumbers = buildList {
                remote.banks.forEach { remoteBank ->
                    val localBank = localById[remoteBank.bankId] ?: return@forEach
                    val added = (remoteBank.numbers - localBank.numbers).sorted()
                    if (added.isNotEmpty()) {
                        add(
                            NumberChange(
                                bankId = remoteBank.bankId,
                                bankName = remoteBank.displayName,
                                numbers = added
                            )
                        )
                    }
                }
            }

            val banksWithRemovedNumbers = buildList {
                local.banks.forEach { localBank ->
                    val remoteBank = remoteById[localBank.bankId] ?: return@forEach
                    val removed = (localBank.numbers - remoteBank.numbers).sorted()
                    if (removed.isNotEmpty()) {
                        add(
                            NumberChange(
                                bankId = localBank.bankId,
                                bankName = localBank.displayName,
                                numbers = removed
                            )
                        )
                    }
                }
            }

            return CatalogDiff(
                localVersion = localVersion,
                remoteVersion = remoteVersion,
                addedBanks = addedBanks,
                removedBanks = removedBanks,
                banksWithAddedNumbers = banksWithAddedNumbers,
                banksWithRemovedNumbers = banksWithRemovedNumbers
            )
        }
    }
}
