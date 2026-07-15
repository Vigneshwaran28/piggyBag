package com.titanbag.app.data

sealed class SyncError(val message: String, val internalLog: String) {
    class Authentication(log: String) : SyncError("Your session expired. Please login again.", log)
    class Network(log: String) : SyncError("Unable to connect. Please check your internet connection.", log)
    class Server(log: String) : SyncError("Sync service temporarily unavailable.", log)
    class Unknown(log: String) : SyncError("An unexpected error occurred during sync.", log)
    class NotLoggedIn : SyncError("User not logged in.", "SessionManager.isLoggedIn returned false")
}
