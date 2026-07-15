package com.titanbag.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.titanbag.app.data.CloudJournalViewModel
import com.titanbag.app.data.CloudAuthState
import com.titanbag.app.data.UserEntity
import com.titanbag.app.BuildConfig
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.exceptions.*
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLoginScreen(
    viewModel: CloudJournalViewModel,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    // When already logged in, show account overview screen (not shared journals)
    when (authState) {
        is CloudAuthState.LoggedIn -> {
            val user = (authState as CloudAuthState.LoggedIn).user
            CloudDashboardContent(
                user = user,
                viewModel = viewModel,
                onBack = onBack
            )
        }
        is CloudAuthState.Loading -> {
            LoadingScreen(message = "Signing in...")
        }
        else -> {
            LoginForm(
                viewModel = viewModel,
                authState = authState,
                onNavigateToRegister = onNavigateToRegister,
                onBack = onBack,
                onLoginSuccess = onLoginSuccess
            )
        }
    }

    // Registration/Login callbacks explicitly handle navigation success.
    // Removed LaunchedEffect(authState) loop that caused instant dismissal on click.
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginForm(
    viewModel: CloudJournalViewModel,
    authState: CloudAuthState,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isProcessing by viewModel.isAuthProcessing.collectAsState()
    val authError = (authState as? CloudAuthState.LoginFailed)?.error
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Account Login", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudQueue,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(96.dp)
                    .padding(8.dp)
            )

            Text(
                text = "Welcome to TitanBag Sync",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sign in to synchronize expense journals with your partner securely in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            authError?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            viewModel.setAuthProcessing(true)
                            viewModel.setAuthError(null)
                            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                            
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(serverClientId)
                                .setAutoSelectEnabled(false)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential
                            
                            android.util.Log.d("CloudAuth", "Credential received: ${credential.type}")

                            val idToken = when {
                                credential is GoogleIdTokenCredential -> credential.idToken
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                                    try {
                                        val parsed = GoogleIdTokenCredential.createFrom(credential.data)
                                        parsed.idToken
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                else -> null
                            }

                            if (!idToken.isNullOrBlank()) {
                                val displayName = (credential as? GoogleIdTokenCredential)?.displayName
                                val photoUrl = (credential as? GoogleIdTokenCredential)?.profilePictureUri?.toString()

                                viewModel.loginWithGoogle(idToken, displayName, photoUrl) { success, error ->
                                    if (success) {
                                        onLoginSuccess()
                                    } else {
                                        android.util.Log.e("CloudAuth", "Backend login failed: $error")
                                        viewModel.setAuthError("Server login failed: $error")
                                    }
                                }
                            } else {
                                viewModel.setAuthProcessing(false)
                                viewModel.setAuthError("Google Login failed: Could not retrieve ID token. (Type: ${credential.type})")
                            }
                        } catch (e: GetCredentialException) {
                            viewModel.setAuthProcessing(false)
                            android.util.Log.e("CloudAuth", "GetCredentialException: ${e.message}", e)
                            when (e) {
                                is GetCredentialCancellationException -> {
                                    // User cancelled
                                }
                                is NoCredentialException -> {
                                    viewModel.setAuthError("No accounts found on this device.")
                                }
                                else -> {
                                    val errorMsg = e.message ?: "Unknown error"
                                    if (errorMsg.contains("28444")) {
                                        viewModel.setAuthError("Developer console error [28444]: SHA-1 or Package Name mismatch. Please verify your Google Cloud Console setup.")
                                    } else {
                                        viewModel.setAuthError("Credential error: $errorMsg")
                                    }
                                }
                            }
                        } catch (e: GoogleIdTokenParsingException) {
                            viewModel.setAuthProcessing(false)
                            viewModel.setAuthError("Failed to parse Google ID token.")
                        } catch (e: Exception) {
                            viewModel.setAuthProcessing(false)
                            viewModel.setAuthError("An unexpected error occurred during Google Sign-In.")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isProcessing && authError == null) {
                         CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Email or Username") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (identifier.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(identifier, password) { success, _ ->
                            if (success) {
                                onLoginSuccess()
                            }
                        }
                    }
                },
                enabled = identifier.isNotBlank() && password.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Log In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isProcessing
            ) {
                Text("Don't have a cloud account? Sign Up")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDashboardContent(
    user: UserEntity,
    viewModel: CloudJournalViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profilePhoto.isNullOrBlank()) {
                    AsyncImage(
                        model = user.profilePhoto,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(user.displayName.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge)
                }
            }

            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account Information", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow("User ID", user.id.take(8) + "...")
                    DetailRow("Username", user.username)
                    DetailRow("Status", "Connected to TitanBag Cloud")
                    DetailRow("Joined", user.createdAt.split("T")[0])
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Return to App")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LoadingScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(message)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudRegisterScreen(
    viewModel: CloudJournalViewModel,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }

    val isProcessing by viewModel.isAuthProcessing.collectAsState()
    val authError by viewModel.authError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Cloud Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.AppRegistration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(96.dp)
                    .padding(8.dp)
            )

            Text(
                text = "Create Cloud Profile",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            val displayError = localError ?: authError
            displayError?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name (e.g. John)") },
                leadingIcon = { Icon(Icons.Rounded.Face, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (Min 8 chars)") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    localError = null
                    if (displayName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
                        localError = "Please fill in all fields"
                        return@Button
                    }
                    if (password.length < 8) {
                        localError = "Password must be at least 8 characters long"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        localError = "Passwords do not match"
                        return@Button
                    }

                    viewModel.register(username, email, password, displayName) { success, _ ->
                        if (success) {
                            onRegisterSuccess()
                        }
                    }
                },
                enabled = displayName.isNotBlank() && username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Register Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            TextButton(
                onClick = onNavigateToLogin,
                enabled = !isProcessing
            ) {
                Text("Already have a cloud account? Sign In")
            }
        }
    }
}
