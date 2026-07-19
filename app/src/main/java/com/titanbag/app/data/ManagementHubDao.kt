package com.titanbag.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class GroupExpenseWithMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val amount: Double,
    val description: String,
    val expenseDate: String,
    val createdAt: String,
    val memberName: String,
    val category: String = "",
    val subcategory: String = "",
    val receipt: String = "",
    val location: String = "",
    val paymentMethod: String = "",
    val lastModified: String = "",
    val tags: String = "",
    val participantsIncluded: String = "",
    val splitType: String = "Equal",
    val shares: String = ""
)

@Dao
interface LocalUserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LocalUserProfile)

    @Query("SELECT * FROM local_user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): LocalUserProfile?

    @Query("SELECT * FROM local_user_profiles WHERE partnerShareCode = :shareCode LIMIT 1")
    suspend fun getProfileByShareCode(shareCode: String): LocalUserProfile?

    @Query("SELECT * FROM local_user_profiles ORDER BY name ASC")
    fun getAllProfilesFlow(): Flow<List<LocalUserProfile>>

    @Query("SELECT * FROM local_user_profiles ORDER BY name ASC")
    suspend fun getAllProfilesDirect(): List<LocalUserProfile>

    @Delete
    suspend fun deleteProfile(profile: LocalUserProfile)
}

@Dao
interface PartnerConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: PartnerConnection)

    @Query("SELECT * FROM local_partner_connections WHERE userId = :userId")
    fun getConnectionsForUserFlow(userId: String): Flow<List<PartnerConnection>>

    @Query("SELECT * FROM local_partner_connections WHERE userId = :userId")
    suspend fun getConnectionsForUserDirect(userId: String): List<PartnerConnection>

    @Query("SELECT * FROM local_partner_connections WHERE userId = :userId AND partnerUserId = :partnerUserId LIMIT 1")
    suspend fun getConnection(userId: String, partnerUserId: String): PartnerConnection?

    @Query("DELETE FROM local_partner_connections WHERE userId = :userId AND partnerUserId = :partnerUserId")
    suspend fun deleteConnection(userId: String, partnerUserId: String)

    @Query("UPDATE local_partner_connections SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)

    @Query("DELETE FROM local_partner_connections WHERE userId = :userId OR partnerUserId = :userId")
    suspend fun deleteConnectionsForUser(userId: String)
}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): Group?

    @Query("SELECT * FROM groups WHERE groupPin = :pin LIMIT 1")
    suspend fun getGroupByPin(pin: String): Group?

    @Query("""
        SELECT g.* FROM groups g
        INNER JOIN group_members m ON g.id = m.groupId
        WHERE m.userId = :userId
        ORDER BY g.createdDate DESC
    """)
    fun getGroupsForUserFlow(userId: String): Flow<List<Group>>

    @Delete
    suspend fun deleteGroup(group: Group)

    @Query("UPDATE groups SET createdBy = :newUserId WHERE createdBy = :oldUserId")
    suspend fun updateCreatedBy(oldUserId: String, newUserId: String)
}

@Dao
interface GroupMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY displayName ASC")
    fun getMembersForGroupFlow(groupId: String): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY displayName ASC")
    suspend fun getMembersForGroupDirect(groupId: String): List<GroupMember>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getMemberByGroupAndUser(groupId: String, userId: String): GroupMember?

    @Delete
    suspend fun deleteMember(member: GroupMember)

    @Query("UPDATE group_members SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface GroupExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: GroupExpense)

    @Update
    suspend fun updateExpense(expense: GroupExpense)

    @Delete
    suspend fun deleteExpense(expense: GroupExpense)

    @Query("SELECT * FROM group_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): GroupExpense?

    @Query("""
        SELECT e.*, m.displayName as memberName 
        FROM group_expenses e
        INNER JOIN group_members m ON e.groupId = m.groupId AND e.userId = m.userId
        WHERE e.groupId = :groupId
        ORDER BY e.expenseDate DESC
    """)
    fun getExpensesForGroupFlow(groupId: String): Flow<List<GroupExpenseWithMember>>

    @Query("SELECT * FROM group_expenses WHERE groupId = :groupId")
    suspend fun getExpensesForGroupDirect(groupId: String): List<GroupExpense>

    @Query("UPDATE group_expenses SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface DebtRecordDao {
    @Query("SELECT * FROM debt_records WHERE userId = :userId ORDER BY borrowedDate DESC")
    fun getDebtRecordsForUserFlow(userId: String): Flow<List<DebtRecord>>

    @Query("SELECT * FROM debt_records WHERE userId = :userId ORDER BY borrowedDate DESC")
    suspend fun getDebtRecordsForUserDirect(userId: String): List<DebtRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtRecord(record: DebtRecord)

    @Update
    suspend fun updateDebtRecord(record: DebtRecord)

    @Delete
    suspend fun deleteDebtRecord(record: DebtRecord)

    @Query("UPDATE debt_records SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface GroupSettlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: GroupSettlement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<GroupSettlement>)

    @Update
    suspend fun updateSettlement(settlement: GroupSettlement)

    @Delete
    suspend fun deleteSettlement(settlement: GroupSettlement)

    @Query("SELECT * FROM group_settlements WHERE groupId = :groupId")
    fun getSettlementsForGroupFlow(groupId: String): Flow<List<GroupSettlement>>

    @Query("SELECT * FROM group_settlements WHERE groupId = :groupId")
    suspend fun getSettlementsForGroupDirect(groupId: String): List<GroupSettlement>

    @Query("DELETE FROM group_settlements WHERE groupId = :groupId")
    suspend fun deleteSettlementsForGroup(groupId: String)
}
