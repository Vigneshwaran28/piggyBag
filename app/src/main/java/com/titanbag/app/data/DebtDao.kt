package com.titanbag.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY date DESC")
    fun getDebtsForUser(userId: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Int): Debt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)
}
