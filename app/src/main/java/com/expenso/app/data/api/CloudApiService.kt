package com.expenso.app.data.api

import retrofit2.Response
import retrofit2.http.*

interface CloudApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @POST("partner/connect")
    suspend fun connectPartner(@Body request: ConnectPartnerRequest): Response<GeneralResponse>

    @DELETE("partner/disconnect")
    suspend fun disconnectPartner(): Response<GeneralResponse>

    @POST("sync")
    suspend fun syncJournals(@Body request: SyncRequest): Response<SyncResponse>
}
