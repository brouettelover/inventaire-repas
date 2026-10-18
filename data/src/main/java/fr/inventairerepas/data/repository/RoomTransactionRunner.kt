package fr.inventairerepas.data.repository

import androidx.room.withTransaction
import fr.inventairerepas.data.db.AppDatabase
import fr.inventairerepas.domain.repository.TransactionRunner
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase,
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
