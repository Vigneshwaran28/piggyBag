package com.titanbag.app.data.api

import retrofit2.Response
import retrofit2.http.*

interface CloudApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @POST("partner/connect")
    suspend fun connectPartner(@Body request: ConnectPartnerRequest): Response<GeneralResponse>

    @DELETE("partner/disconnect")
    suspend fun disconnectPartner(): Response<GeneralResponse>

    @POST("api/partner/block")
    suspend fun blockPartner(): Response<GeneralResponse>


    @POST("sync")
    suspend fun syncJournals(@Body request: SyncRequest): Response<SyncResponse>

    // --- SHARED JOURNALS ---

    @POST("api/journals")
    suspend fun createSharedJournal(@Body request: CreateJournalRequest): Response<SharedJournalDto>

    @POST("api/journals/join")
    suspend fun joinSharedJournal(@Body request: JoinJournalRequest): Response<GeneralResponse>

    @GET("api/journals")
    suspend fun getMySharedJournals(): Response<List<SharedJournalDto>>

    @GET("api/journals/{id}")
    suspend fun getJournalDetails(@Path("id") journalId: String): Response<JournalFullDetailsResponse>

    @POST("api/journals/{id}/transactions")
    suspend fun addJournalTransaction(
        @Path("id") journalId: String,
        @Body transaction: SharedJournalTransactionDto
    ): Response<SharedJournalTransactionDto>

    // --- ACCOUNTS ---

    @GET("api/accounts")
    suspend fun getAccounts(): Response<List<AccountDto>>

    @POST("api/accounts")
    suspend fun createAccount(@Body request: AccountRequest): Response<AccountDto>

    @PUT("api/accounts/{id}")
    suspend fun updateAccount(@Path("id") id: Int, @Body request: AccountRequest): Response<AccountDto>

    @DELETE("api/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Int): Response<GeneralResponse>

    // --- BUDGETS ---

    @GET("api/budgets")
    suspend fun getBudgets(): Response<List<BudgetDto>>

    @POST("api/budgets")
    suspend fun createBudget(@Body request: BudgetRequest): Response<BudgetDto>

    @PUT("api/budgets/{id}")
    suspend fun updateBudget(@Path("id") id: Int, @Body request: BudgetRequest): Response<BudgetDto>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Int): Response<GeneralResponse>

    // --- SAVINGS GOALS ---

    @GET("api/savings-goals")
    suspend fun getSavingsGoals(): Response<List<SavingsGoalDto>>

    @POST("api/savings-goals")
    suspend fun createSavingsGoal(@Body request: SavingsGoalRequest): Response<SavingsGoalDto>

    @PUT("api/savings-goals/{id}")
    suspend fun updateSavingsGoal(@Path("id") id: Int, @Body request: SavingsGoalRequest): Response<SavingsGoalDto>

    @DELETE("api/savings-goals/{id}")
    suspend fun deleteSavingsGoal(@Path("id") id: Int): Response<GeneralResponse>

    // --- RECURRING TRANSACTIONS ---

    @GET("api/recurring-transactions")
    suspend fun getRecurringTransactions(): Response<List<RecurringTransactionDto>>

    @POST("api/recurring-transactions")
    suspend fun createRecurringTransaction(@Body request: RecurringTransactionRequest): Response<RecurringTransactionDto>

    @PUT("api/recurring-transactions/{id}")
    suspend fun updateRecurringTransaction(@Path("id") id: Int, @Body request: RecurringTransactionRequest): Response<RecurringTransactionDto>

    @DELETE("api/recurring-transactions/{id}")
    suspend fun deleteRecurringTransaction(@Path("id") id: Int): Response<GeneralResponse>

    // --- SETTINGS ---

    @GET("api/settings")
    suspend fun getSettings(): Response<SettingsDto>

    @POST("api/settings")
    suspend fun saveSettings(@Body request: SettingsRequest): Response<SettingsDto>

    // --- DEBT RECORDS ---

    @GET("api/debts")
    suspend fun getDebtRecords(): Response<List<DebtRecordDto>>

    @POST("api/debts")
    suspend fun createDebtRecord(@Body request: DebtRecordRequest): Response<DebtRecordDto>

    @PUT("api/debts/{id}")
    suspend fun updateDebtRecord(@Path("id") id: String, @Body request: DebtRecordRequest): Response<DebtRecordDto>

    @DELETE("api/debts/{id}")
    suspend fun deleteDebtRecord(@Path("id") id: String): Response<GeneralResponse>

    // --- CATEGORIES ---

    @GET("api/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @POST("api/categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<CategoryDto>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body request: CategoryRequest): Response<CategoryDto>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<GeneralResponse>
}

