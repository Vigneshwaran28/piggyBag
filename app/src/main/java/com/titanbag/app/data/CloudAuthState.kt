package com.titanbag.app.data

sealed class CloudAuthState {
    object CheckingSession : CloudAuthState()
    object LoggedOut : CloudAuthState()
    data class LoggedIn(val user: UserEntity) : CloudAuthState()
    data class LoginFailed(val error: String) : CloudAuthState()
    object Loading : CloudAuthState()
}
