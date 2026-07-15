package com.titanbag.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM cloud_users LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM cloud_users LIMIT 1")
    suspend fun getCurrentUserDirect(): UserEntity?

    @Query("DELETE FROM cloud_users")
    suspend fun clearUser()
}

@Dao
interface CloudPartnerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerEntity)

    @Query("SELECT * FROM cloud_partners LIMIT 1")
    fun getPartnerRelationFlow(): Flow<PartnerEntity?>

    @Query("DELETE FROM cloud_partners")
    suspend fun clearPartner()
}

@Dao
interface CloudJournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournals(journals: List<JournalEntity>)

    @Query("SELECT * FROM cloud_journals WHERE id = :id")
    suspend fun getJournalById(id: String): JournalEntity?

    @Query("SELECT * FROM cloud_journals WHERE deleted = 0 ORDER BY date DESC")
    fun getAllJournalsFlow(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM cloud_journals WHERE deleted = 0 ORDER BY date DESC")
    suspend fun getAllJournalsDirect(): List<JournalEntity>

    @Query("SELECT * FROM cloud_journals WHERE syncStatus != 'SYNCED'")
    suspend fun getSyncPendingJournals(): List<JournalEntity>

    @Query("DELETE FROM cloud_journals WHERE id = :id")
    suspend fun deleteJournalById(id: String)

    @Query("DELETE FROM cloud_journals")
    suspend fun clearAll()
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY id ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Update
    suspend fun updateQueueItem(item: SyncQueueEntity)

    @Delete
    suspend fun deleteQueueItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE entityId = :entityId")
    suspend fun deleteQueueItemByEntityId(entityId: String)

    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId LIMIT 1")
    suspend fun getQueueItemByEntityId(entityId: String): SyncQueueEntity?
}

@Dao
interface SharedJournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: SharedJournalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournals(journals: List<SharedJournalEntity>)

    @Query("SELECT * FROM shared_journals ORDER BY updatedAt DESC")
    fun getAllJournalsFlow(): Flow<List<SharedJournalEntity>>

    @Query("SELECT * FROM shared_journals WHERE id = :id")
    suspend fun getJournalById(id: String): SharedJournalEntity?

    @Query("DELETE FROM shared_journals")
    suspend fun clearAll()
}

@Dao
interface SharedJournalTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SharedJournalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<SharedJournalTransactionEntity>)

    @Query("SELECT * FROM shared_journal_transactions WHERE journalId = :journalId ORDER BY date DESC")
    fun getTransactionsForJournalFlow(journalId: String): Flow<List<SharedJournalTransactionEntity>>

    @Query("DELETE FROM shared_journal_transactions WHERE journalId = :journalId")
    suspend fun deleteTransactionsForJournal(journalId: String)

    @Query("DELETE FROM shared_journal_transactions")
    suspend fun clearAll()
}

