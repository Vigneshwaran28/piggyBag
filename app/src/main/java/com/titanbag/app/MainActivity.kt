package com.titanbag.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.absoluteValue
import com.titanbag.app.data.*
import com.titanbag.app.ui.screens.*
import com.titanbag.app.ui.theme.MyApplicationTheme

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private val viewModel: TitanBagViewModel by viewModels()
    private val cloudViewModel: CloudJournalViewModel by viewModels()

    // Export CSV Contract
    private val csvExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val outStream = contentResolver.openOutputStream(uri)
                if (outStream != null) {
                    viewModel.exportTransactionsToCsv(outStream) { success ->
                        runOnUiThread {
                            if (success) {
                                viewModel.showSnackbar("Transactions exported to CSV! 📊")
                            } else {
                                viewModel.showSnackbar("Failed to export CSV.")
                            }
                        }
                    }
                } else {
                    viewModel.showSnackbar("Failed to open CSV destination file.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showSnackbar("CSV Export Error: ${e.message}")
            }
        }
    }

    // Export PDF Contract
    private val pdfExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val outStream = contentResolver.openOutputStream(uri)
                if (outStream != null) {
                    viewModel.exportTransactionsToPdf(outStream) { success ->
                        runOnUiThread {
                            if (success) {
                                viewModel.showSnackbar("Transactions exported to PDF! 📄")
                            } else {
                                viewModel.showSnackbar("Failed to export PDF.")
                            }
                        }
                    }
                } else {
                    viewModel.showSnackbar("Failed to open PDF destination file.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showSnackbar("PDF Export Error: ${e.message}")
            }
        }
    }

    // Import CSV Contract
    private val csvImportLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inStream = contentResolver.openInputStream(uri)
                if (inStream != null) {
                    viewModel.importTransactionsFromCsv(inStream) { success, message ->
                        runOnUiThread {
                            if (success) {
                                viewModel.showSnackbar(message)
                            } else {
                                viewModel.showSnackbar("CSV Import Failed: $message")
                            }
                        }
                    }
                } else {
                    viewModel.showSnackbar("Failed to open CSV source file.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.showSnackbar("CSV Import Error: ${e.message}")
            }
        }
    }

    private var isRequestingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val cloudUser by cloudViewModel.currentUser.collectAsState(initial = null)
            val fontStyle by viewModel.fontStyle.collectAsState()

            // Handle permissions in a safe way
            LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (!hasNotificationPermission) {
                        isRequestingPermission = true
                        androidx.core.app.ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            101
                        )
                        // Reset after a delay or in onRequestPermissionsResult
                    }
                }
            }

            LaunchedEffect(cloudUser) {
                val activeUserId = viewModel.currentUserId.value
                val hasSecureSession = cloudViewModel.sessionManager.isLoggedIn()
                val secureUserId = cloudViewModel.sessionManager.getUserId()
                if (cloudUser != null) {
                    if (activeUserId != cloudUser!!.id) {
                        viewModel.switchLocalUser(cloudUser!!.id, showFeedback = false)
                    }
                } else {
                    if (!hasSecureSession || secureUserId == null) {
                        if (activeUserId != "default_user") {
                            viewModel.switchLocalUser("default_user", showFeedback = false)
                        }
                    } else {
                        if (activeUserId != secureUserId) {
                            viewModel.switchLocalUser(secureUserId, showFeedback = false)
                        }
                    }
                }
            }
            
            // Map settings ThemeMode into Compose isDarkTheme logic
            val isDark = when (settings?.themeMode) {
                "light" -> false
                "dark", "soft_dark", "pure_black" -> true
                else -> isSystemInDarkTheme()
            }

            val visualStyle by viewModel.visualStyle.collectAsState()

            MyApplicationTheme(
                darkTheme = isDark,
                colorPalette = settings?.colorPalette ?: "Default",
                customColorHex = settings?.customColor,
                customIconColorHex = settings?.customIconColor,
                customBgColorHex = settings?.customBgColor,
                themeModeSetting = settings?.themeMode ?: "system",
                fontStyle = fontStyle,
                visualStyle = visualStyle
            ) {
                val isLocked by viewModel.isLocked.collectAsState()

                if (isLocked) {
                    SplashAndLockScreen(
                        viewModel = viewModel,
                        onTriggerBiometric = { onSuccess ->
                            triggerBiometricPrompt {
                                onSuccess()
                            }
                        },
                        onUnlockSuccess = {
                            viewModel.unlockApp()
                        }
                    )
                } else {
                    MainAppContent(
                        viewModel = viewModel,
                        cloudViewModel = cloudViewModel,
                        onExportCsv = { csvExportLauncher.launch("titanbag_transactions.csv") },
                        onImportCsv = { csvImportLauncher.launch("text/comma-separated-values") },
                        onExportPdf = { filename -> pdfExportLauncher.launch(filename) },
                        onShowNotification = { title, msg, dest -> showSystemNotification(title, msg, dest) }
                    )
                }
            }
        }

        intent?.getStringExtra("navigate_to")?.let {
            if (isValidNavigationDestination(it)) {
                viewModel.navigateTo(it)
            }
        }
    }

    private fun isValidNavigationDestination(destination: String): Boolean {
        val validDestinations = listOf(
            "debt_list", "reminders", "autopay", "categories", "budgets", 
            "accounts", "analytics", "records", "search_transactions"
        )
        return validDestinations.contains(destination)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("navigate_to")?.let {
            if (isValidNavigationDestination(it)) {
                viewModel.navigateTo(it)
            }
        }
    }

    private fun triggerBiometricPrompt(onSuccess: () -> Unit) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("PiggyBag Secure Unlock")
            .setSubtitle("Authenticate using biometrics to access your financial vault")
            .setNegativeButtonText("Use PIN / Cancel")
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Biometrics not set up or not supported
        }
    }

    private fun showSystemNotification(title: String, message: String, destination: String? = null) {
        val channelId = "titanbag_alerts"
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "PiggyBag Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alert notifications for PiggyBag app"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destination != null) {
                putExtra("navigate_to", destination)
            }
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Avoid auto-locking when a system dialog (like permissions) is shown, 
        // which erroneously triggers onUserLeaveHint on some Samsung devices.
        if (!isRequestingPermission) {
            viewModel.lockApp()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            isRequestingPermission = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: TitanBagViewModel,
    cloudViewModel: CloudJournalViewModel,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onExportPdf: (String) -> Unit,
    onShowNotification: (String, String, String?) -> Unit = { _, _, _ -> }
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    // Quick log prefill states
    var selectedQuickLogVehicleId by remember { mutableStateOf<Int?>(null) }
    var selectedQuickLogCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedQuickLogSubcategoryId by remember { mutableStateOf<Int?>(null) }

    // Form navigation overlays
    val activeFormStack = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }
    var selectedTransactionToEdit by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var lastNonNullTxToEdit by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var selectedAccountToEdit by remember { mutableStateOf<Account?>(null) }
    var lastNonNullAccToEdit by remember { mutableStateOf<Account?>(null) }
    var selectedGoalToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var lastNonNullGoalToEdit by remember { mutableStateOf<SavingsGoal?>(null) }
    var selectedCategoryToEdit by remember { mutableStateOf<com.titanbag.app.data.Category?>(null) }
    var lastNonNullCategoryToEdit by remember { mutableStateOf<com.titanbag.app.data.Category?>(null) }
    var selectedBudgetToEdit by remember { mutableStateOf<com.titanbag.app.data.Budget?>(null) }
    var lastNonNullBudgetToEdit by remember { mutableStateOf<com.titanbag.app.data.Budget?>(null) }
    var selectedCloudJournalToEdit by remember { mutableStateOf<JournalEntity?>(null) }
    var lastNonNullCloudJournalToEdit by remember { mutableStateOf<JournalEntity?>(null) }
    var selectedSharedJournalId by remember { mutableStateOf<String?>(null) }

    val activeFormState = remember {
        object : MutableState<String?> {
            override var value: String?
                get() = activeFormStack.lastOrNull()
                set(newValue) {
                    if (newValue == null) {
                        activeFormStack.clear()
                        selectedTransactionToEdit = null
                        selectedAccountToEdit = null
                        selectedGoalToEdit = null
                        selectedCategoryToEdit = null
                        selectedBudgetToEdit = null
                    } else {
                        val index = activeFormStack.indexOf(newValue)
                        if (index >= 0) {
                            // Clear states of popped screens
                            for (i in (index + 1) until activeFormStack.size) {
                                when (activeFormStack[i]) {
                                    "edit_transaction" -> selectedTransactionToEdit = null
                                    "edit_account" -> selectedAccountToEdit = null
                                    "edit_category" -> selectedCategoryToEdit = null
                                    "edit_goal" -> selectedGoalToEdit = null
                                    "edit_budget" -> selectedBudgetToEdit = null
                                    "edit_cloud_journal" -> selectedCloudJournalToEdit = null
                                }
                            }
                            while (activeFormStack.size > index + 1) {
                                activeFormStack.removeAt(activeFormStack.lastIndex)
                            }
                        } else {
                            activeFormStack.add(newValue)
                        }
                    }
                }

            override fun component1(): String? = value
            override fun component2(): (String?) -> Unit = { value = it }
        }
    }
    var activeForm by activeFormState
    var previousForm by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeForm != null || currentTab != 0) {
        if (activeForm != null) {
            if (activeFormStack.size > 1) {
                val popped = activeFormStack.removeAt(activeFormStack.lastIndex)
                // Clear state of the popped screen
                when (popped) {
                    "edit_transaction" -> selectedTransactionToEdit = null
                    "edit_account" -> selectedAccountToEdit = null
                    "edit_category" -> selectedCategoryToEdit = null
                    "edit_goal" -> selectedGoalToEdit = null
                    "edit_budget" -> selectedBudgetToEdit = null
                    "edit_cloud_journal" -> selectedCloudJournalToEdit = null
                }
            } else {
                activeFormStack.clear()
                selectedTransactionToEdit = null
                selectedAccountToEdit = null
                selectedGoalToEdit = null
                selectedCategoryToEdit = null
                selectedBudgetToEdit = null
                selectedCloudJournalToEdit = null
            }
        } else if (currentTab != 0) {
            viewModel.selectTab(0)
        }
    }

    // Listen to localized alerts / budget warnings from SharedFlow
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.notificationMessage.collect { event ->
            viewModel.showSnackbar(event.message)
            onShowNotification(event.title, event.message, event.destination)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationCommand.collect { destination ->
            if (destination == "debt_list") {
                viewModel.selectTab(4)
                activeForm = "debt_list"
            }
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { 
                com.titanbag.app.ui.components.SwipeableSnackbarHost(
                    hostState = snackbarHostState
                )
            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                val isScrolling by viewModel.isScrolling.collectAsState()
                AnimatedVisibility(
                    visible = currentTab == 0 && !isScrolling,
                    enter = slideInVertically(
                        initialOffsetY = { it * 2 },
                        animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                    ) + fadeIn(
                        animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it * 2 },
                        animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                    ) + fadeOut(
                        animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                    ),
                    modifier = Modifier.zIndex(0f)
                ) {
                    FloatingActionButton(
                        onClick = { activeForm = "add_transaction" },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
                    }
                }
            },
            bottomBar = {
                val defaultTabs = listOf("Home", "Analytics", "Budgets", "Accounts", "More")
                val configuredTabs = remember(settings) {
                    settings?.bottomTabs?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: defaultTabs
                }
                
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    configuredTabs.take(5).forEachIndexed { idx, tab ->
                        val icon = when (tab) {
                            "Home" -> Icons.Rounded.Home
                            "Analytics" -> Icons.Rounded.BarChart
                            "Budgets" -> Icons.Rounded.DonutLarge
                            "Accounts" -> Icons.Rounded.AccountBalance
                            "Transactions" -> Icons.Rounded.List
                            "Debt" -> Icons.Rounded.ListAlt
                            "Partner Sharing" -> Icons.Rounded.People
                            "Group Expenses" -> Icons.Rounded.Group
                            "AutoPay" -> Icons.Rounded.Update
                            "Investments" -> Icons.AutoMirrored.Rounded.ShowChart
                            "Subscriptions" -> Icons.Rounded.CardMembership
                            "Categories" -> Icons.Rounded.Category
                            else -> Icons.Rounded.MoreHoriz
                        }
                        
                        NavigationBarItem(
                            selected = currentTab == idx,
                            onClick = { viewModel.selectTab(idx) },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(tab, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
                            modifier = Modifier.testTag("tab_${tab.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(
                        bottom = innerPadding.calculateBottomPadding()
                    )
                    .fillMaxSize()
            ) {
                val defaultTabs = listOf("Home", "Analytics", "Budgets", "Accounts", "More")
                val configuredTabs = remember(settings) {
                    settings?.bottomTabs?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: defaultTabs
                }

                androidx.compose.animation.AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val slideSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween<androidx.compose.ui.unit.IntOffset>()
                        val scaleSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween<Float>()
                        val fadeSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween<Float>()
                        
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> width / 12 } +
                             scaleIn(animationSpec = scaleSpec, initialScale = 0.98f) +
                             fadeIn(animationSpec = fadeSpec))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = slideSpec) { width -> -width / 12 } +
                                    scaleOut(animationSpec = scaleSpec, targetScale = 0.98f) +
                                    fadeOut(animationSpec = fadeSpec)
                                )
                        } else if (targetState < initialState) {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> -width / 12 } +
                             scaleIn(animationSpec = scaleSpec, initialScale = 0.98f) +
                             fadeIn(animationSpec = fadeSpec))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = slideSpec) { width -> width / 12 } +
                                    scaleOut(animationSpec = scaleSpec, targetScale = 0.98f) +
                                    fadeOut(animationSpec = fadeSpec)
                                )
                        } else {
                            fadeIn(animationSpec = fadeSpec).togetherWith(fadeOut(animationSpec = fadeSpec))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "tab_animated_content"
                ) { page ->
                    val tabName = configuredTabs.getOrNull(page) ?: "Home"
                    when (tabName) {
                        "Home" -> {
                            RecordsScreen(
                                viewModel = viewModel,
                                onEditTransaction = { tx ->
                                    previousForm = null
                                    selectedTransactionToEdit = tx
                                    lastNonNullTxToEdit = tx
                                    activeForm = "edit_transaction"
                                },
                                onAddTransactionClick = { activeForm = "add_transaction" },
                                onSearchClick = { activeForm = "search_transactions" }
                            )
                        }
                        "Analytics" -> {
                            AnalysisScreen(
                                viewModel = viewModel,
                                onSearchClick = { activeForm = "search_transactions" }
                            )
                        }
                        "Budgets" -> {
                            BudgetsScreen(
                                viewModel = viewModel,
                                onNavigateToAddBudget = { activeForm = "add_budget" },
                                onEditBudget = { budget ->
                                    selectedBudgetToEdit = budget
                                    lastNonNullBudgetToEdit = budget
                                    activeForm = "edit_budget"
                                }
                            )
                        }
                        "Accounts" -> {
                            AccountsScreen(
                                viewModel = viewModel,
                                onAddAccountClick = { activeForm = "add_account" },
                                onEditAccount = { account ->
                                    selectedAccountToEdit = account
                                    lastNonNullAccToEdit = account
                                    activeForm = "edit_account"
                                },
                                onAddTransactionClick = { _ ->
                                    activeForm = "add_transaction"
                                }
                            )
                        }
                        "Transactions" -> {
                            RecordsScreen(
                                viewModel = viewModel,
                                onEditTransaction = { tx ->
                                    previousForm = null
                                    selectedTransactionToEdit = tx
                                    lastNonNullTxToEdit = tx
                                    activeForm = "edit_transaction"
                                },
                                onAddTransactionClick = { activeForm = "add_transaction" },
                                onSearchClick = { activeForm = "search_transactions" }
                            )
                        }
                        "Debt" -> {
                            DebtListScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.selectTab(0) }
                            )
                        }
                        "Partner Sharing" -> {
                            PartnerSharingScreen(
                                viewModel = viewModel,
                                cloudViewModel = cloudViewModel,
                                onBack = { viewModel.selectTab(0) }
                            )
                        }
                        "Group Expenses" -> {
                            GroupExpenseSplitScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.selectTab(0) }
                            )
                        }
                        "AutoPay" -> {
                            AutoPayScreen(
                                viewModel = viewModel,
                                onDismiss = { viewModel.selectTab(0) }
                            )
                        }
                        "Investments" -> {
                            InvestmentScreen(
                                viewModel = viewModel,
                                onDismiss = { viewModel.selectTab(0) }
                            )
                        }
                        "Subscriptions" -> {
                            SubscriptionScreen(
                                viewModel = viewModel,
                                onDismiss = { viewModel.selectTab(0) }
                            )
                        }
                        "Categories" -> {
                            CategoriesScreen(
                                viewModel = viewModel,
                                onNavigateToAddCategory = {
                                    activeForm = "add_category"
                                },
                                onEditCategory = { category ->
                                    selectedCategoryToEdit = category
                                    lastNonNullCategoryToEdit = category
                                    activeForm = "edit_category"
                                },
                                onDismiss = { viewModel.selectTab(0) }
                            )
                        }
                        else -> { // More
                            MoreScreen(
                                viewModel = viewModel,
                                cloudViewModel = cloudViewModel,
                                onExportCsv = onExportCsv,
                                onImportCsv = onImportCsv,
                                onExportPdf = onExportPdf,
                                onNavigateToCategories = { activeForm = "categories" },
                                onNavigateToAddBudget = { activeForm = "add_budget" },
                                onNavigateToAddRecurring = { activeForm = "add_recurring" },
                                onNavigateToCloudDashboard = { activeForm = "cloud_dashboard" },
                                onNavigateToCloudPartner = { activeForm = "cloud_partner" },
                                onNavigateToCloudSync = { activeForm = "cloud_sync" },
                                onNavigateToCloudLogin = { activeForm = "cloud_login" },
                                onNavigateToCloudShare = { activeForm = "cloud_share" },
                                onNavigateToFinanceDashboard = { activeForm = "finance_dashboard" },
                                onNavigateToPartnerSharing = { activeForm = "partner_sharing" },
                                onNavigateToGroupExpenseSplit = { activeForm = "group_expense_split" },
                                onNavigateToDebtList = { activeForm = "debt_list" },
                                onNavigateToThemeCustomization = { activeForm = "theme_customization" },
                                onNavigateToHelpGuide = { activeForm = "help_guide" },

                                onNavigateToReminders = { activeForm = "reminders" },
                                onNavigateToAutoPay = { activeForm = "autopay" },
                                onNavigateToNavigationCustomization = { activeForm = "navigation_customization" }
                            )
                        }
                    }
                }
            }
        }

        // FULL SCREEN OVERLAY TRANSITIONAL MODAL DIALOGS FOR FORMS

        AnimatedContent(
            targetState = activeForm,
            transitionSpec = {
                val isHorizontalState = { state: String? ->
                    state != null && state != "add_transaction" && state != "search_transactions" && state != "finance_dashboard" && state != "cloud_share"
                }
                
                if (initialState == null && targetState != null) {
                    val enterAnim = if (targetState in listOf("add_transaction", "edit_transaction", "add_category", "edit_category")) {
                        scaleIn(
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.9f),
                            initialScale = 0.82f,
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                        ) + fadeIn(
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                        )
                    } else if (isHorizontalState(targetState)) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                        ) + fadeIn(
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                        )
                    } else {
                        slideInVertically(
                            initialOffsetY = { (it * 0.3f).toInt() },
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                        ) + fadeIn(
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                        )
                    }
                    enterAnim togetherWith fadeOut(animationSpec = tween(0))
                } else if (initialState != null && targetState == null) {
                    val exitAnim = if (initialState in listOf("add_transaction", "edit_transaction", "add_category", "edit_category")) {
                        scaleOut(
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.9f),
                            targetScale = 0.82f,
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                        ) + fadeOut(
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                        )
                    } else if (isHorizontalState(initialState)) {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                        ) + fadeOut(
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                        )
                    } else {
                        slideOutVertically(
                            targetOffsetY = { (it * 0.3f).toInt() },
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                        ) + fadeOut(
                            animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween()
                        )
                    }
                    fadeIn(animationSpec = tween(0)) togetherWith exitAnim
                } else {
                    val getDepth = { state: String? ->
                        when (state) {
                            null -> 0
                            "search_transactions", "categories", "add_transaction", "add_budget", "edit_budget", "add_goal", "edit_goal", "add_recurring", "edit_recurring" -> 1
                            "edit_transaction" -> 2
                            "add_category", "edit_category", "add_account", "edit_account" -> 3
                            else -> 1
                        }
                    }
                    val isBack = getDepth(initialState) > getDepth(targetState)
                    val slideSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring<androidx.compose.ui.unit.IntOffset>()
                    val fadeSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultTween<Float>()
                    val isScaleState = { state: String? ->
                        state in listOf("add_transaction", "edit_transaction", "add_category", "edit_category")
                    }
                    
                    if (isBack) {
                        if (isScaleState(initialState)) {
                            val exitAnim = scaleOut(
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.9f),
                                targetScale = 0.82f,
                                animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                            ) + fadeOut(animationSpec = fadeSpec)
                            fadeIn(animationSpec = fadeSpec) togetherWith exitAnim
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> -width / 4 } + fadeIn(animationSpec = fadeSpec)) togetherWith
                            (slideOutHorizontally(animationSpec = slideSpec) { width -> width } + fadeOut(animationSpec = fadeSpec))
                        }
                    } else {
                        if (isScaleState(targetState)) {
                            val enterAnim = scaleIn(
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.9f),
                                initialScale = 0.82f,
                                animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring()
                            ) + fadeIn(animationSpec = fadeSpec)
                            enterAnim togetherWith fadeOut(animationSpec = fadeSpec)
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> width } + fadeIn(animationSpec = fadeSpec)) togetherWith
                            (slideOutHorizontally(animationSpec = slideSpec) { width -> -width / 4 } + fadeOut(animationSpec = fadeSpec))
                        }
                    }
                }
            },
            label = "form_transition",
            modifier = Modifier.fillMaxSize()
        ) { targetForm ->
            if (targetForm != null) {
                when (targetForm) {
                    "search_transactions" -> {
                        SearchScreen(
                            viewModel = viewModel,
                            onEditTransaction = { tx ->
                                previousForm = "search_transactions"
                                selectedTransactionToEdit = tx
                                lastNonNullTxToEdit = tx
                                activeForm = "edit_transaction"
                            },
                            onDismiss = { activeForm = null }
                        )
                    }
                    "add_transaction" -> {
                        TransactionForm(
                            viewModel = viewModel,
                            initialVehicleId = selectedQuickLogVehicleId,
                            initialCategoryId = selectedQuickLogCategoryId,
                            initialSubcategoryId = selectedQuickLogSubcategoryId,
                            onNavigateToAddCategory = { activeForm = "add_category" },
                            onNavigateToAddAccount = { activeForm = "add_account" },
                            onDismiss = { 
                                activeForm = null
                                selectedQuickLogVehicleId = null
                                selectedQuickLogCategoryId = null
                                selectedQuickLogSubcategoryId = null
                            }
                        )
                    }
                    "edit_transaction" -> {
                        TransactionForm(
                            viewModel = viewModel,
                            transactionToEdit = lastNonNullTxToEdit,
                            onNavigateToAddCategory = { activeForm = "add_category" },
                            onNavigateToAddAccount = { activeForm = "add_account" },
                            onDismiss = { 
                                activeForm = previousForm
                                previousForm = null
                                selectedTransactionToEdit = null
                            }
                        )
                    }
                    "add_account" -> {
                        AccountForm(
                            viewModel = viewModel,
                            onDismiss = { activeForm = null }
                        )
                    }
                    "edit_account" -> {
                        AccountForm(
                            viewModel = viewModel,
                            accountToEdit = lastNonNullAccToEdit,
                            onDismiss = {
                                activeForm = null
                                selectedAccountToEdit = null
                            }
                        )
                    }
                    "categories" -> {
                        CategoriesScreen(
                            viewModel = viewModel,
                            onNavigateToAddCategory = {
                                previousForm = "categories"
                                activeForm = "add_category"
                            },
                            onEditCategory = { category ->
                                previousForm = "categories"
                                selectedCategoryToEdit = category
                                lastNonNullCategoryToEdit = category
                                activeForm = "edit_category"
                            },
                            onDismiss = { activeForm = null }
                        )
                    }
                    "add_category" -> {
                        CategoryForm(
                            viewModel = viewModel,
                            onDismiss = { 
                                activeForm = previousForm
                                previousForm = null
                            }
                        )
                    }
                    "edit_category" -> {
                        CategoryForm(
                            viewModel = viewModel,
                            categoryToEdit = lastNonNullCategoryToEdit,
                            onDismiss = {
                                activeForm = previousForm
                                previousForm = null
                                selectedCategoryToEdit = null
                            }
                        )
                    }
                    "add_budget" -> {
                        BudgetForm(
                            viewModel = viewModel,
                            onNavigateToAddCategory = {
                                previousForm = "add_budget"
                                activeForm = "add_category"
                            },
                            onDismiss = { activeForm = null }
                        )
                    }
                    "edit_budget" -> {
                        BudgetForm(
                            viewModel = viewModel,
                            budgetToEdit = lastNonNullBudgetToEdit,
                            onNavigateToAddCategory = {
                                previousForm = "edit_budget"
                                activeForm = "add_category"
                            },
                            onDismiss = {
                                activeForm = null
                                selectedBudgetToEdit = null
                            }
                        )
                    }
                    "add_goal" -> {
                        SavingsGoalForm(
                            viewModel = viewModel,
                            onDismiss = { activeForm = null }
                        )
                    }
                    "edit_goal" -> {
                        SavingsGoalForm(
                            viewModel = viewModel,
                            goalToEdit = lastNonNullGoalToEdit,
                            onDismiss = {
                                activeForm = null
                                selectedGoalToEdit = null
                            }
                        )
                    }
                    "add_recurring" -> {
                        RecurringTransactionForm(
                                viewModel = viewModel,
                                onDismiss = { activeForm = null }
                           )
                       }
                       "cloud_login" -> {
                           CloudLoginScreen(
                               viewModel = cloudViewModel,
                               onNavigateToRegister = { activeForm = "cloud_register" },
                               onBack = { activeForm = null },
                               onLoginSuccess = { activeForm = null }
                           )
                       }
                       "cloud_register" -> {
                           CloudRegisterScreen(
                               viewModel = cloudViewModel,
                               onNavigateToLogin = { activeForm = "cloud_login" },
                               onBack = { activeForm = "cloud_login" },
                               onRegisterSuccess = { activeForm = null }
                           )
                       }
                       "cloud_dashboard" -> {
                           CloudDashboardScreen(
                               viewModel = cloudViewModel,
                               onAddJournal = { activeForm = "add_cloud_journal" },
                               onEditJournal = { journal ->
                                   selectedCloudJournalToEdit = journal
                                   lastNonNullCloudJournalToEdit = journal
                                   activeForm = "edit_cloud_journal"
                               },
                               onBack = { activeForm = null }
                           )
                       }
                       "cloud_partner" -> {
                           PartnerProfileScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = null }
                           )
                       }
                       "cloud_sync" -> {
                           SyncStatusScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = null }
                           )
                       }
                       "add_cloud_journal" -> {
                           AddEditCloudJournalScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = "cloud_dashboard" }
                           )
                       }
                       "edit_cloud_journal" -> {
                           AddEditCloudJournalScreen(
                               viewModel = cloudViewModel,
                               journalToEdit = lastNonNullCloudJournalToEdit,
                               onBack = {
                                   activeForm = "cloud_dashboard"
                                   selectedCloudJournalToEdit = null
                               }
                           )
                       }
                       "finance_dashboard" -> {
                           FinanceDashboardScreen(
                               viewModel = viewModel,
                               onBack = { activeForm = null }
                           )
                       }
                         "partner_sharing" -> {
                             PartnerSharingScreen(
                                 viewModel = viewModel,
                                 cloudViewModel = cloudViewModel,
                                 onBack = { activeForm = null }
                             )
                         }
                         "theme_customization" -> {
                             ThemeCustomizationScreen(
                                 viewModel = viewModel,
                                 onBack = { activeForm = null }
                             )
                         }
                         "help_guide" -> {
                             HelpGuideScreen(
                                 onBack = { activeForm = null }
                             )
                         }
                       "group_expense_split" -> {
                           GroupExpenseSplitScreen(
                               viewModel = viewModel,
                               onBack = { activeForm = null }
                           )
                       }
                       "debt_list" -> {
                           DebtListScreen(
                               viewModel = viewModel,
                               onBack = { activeForm = null }
                           )
                       }
                       "cloud_share" -> {
                           CloudShareScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = null },
                               onNavigateToPartner = { activeForm = "cloud_partner" },
                               onNavigateToSharedJournals = { activeForm = "shared_journals" },
                               onNavigateToSyncStatus = { activeForm = "cloud_sync" }
                           )
                       }
                       "shared_journals" -> {
                           SharedJournalListScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = "cloud_share" },
                               onCreateJournal = { activeForm = "create_journal" },
                               onJoinJournal = { activeForm = "join_journal" },
                               onSelectJournal = { id ->
                                   selectedSharedJournalId = id
                                   activeForm = "journal_detail"
                               }
                           )
                       }
                       "create_journal" -> {
                           CreateJournalScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = "shared_journals" },
                               onCreated = { activeForm = "shared_journals" }
                           )
                       }
                       "join_journal" -> {
                           JoinJournalScreen(
                               viewModel = cloudViewModel,
                               onBack = { activeForm = "shared_journals" },
                               onJoined = {
                                   activeForm = "shared_journals"
                               }
                           )
                       }
                       "journal_detail" -> {
                           selectedSharedJournalId?.let { id ->
                               JournalDetailScreen(
                                   journalId = id,
                                   viewModel = cloudViewModel,
                                   onBack = { 
                                       activeForm = "shared_journals"
                                       selectedSharedJournalId = null
                                   },
                                   onAddTransaction = { id ->
                                       activeForm = "add_shared_journal_tx"
                                   }
                               )
                           }
                       }
                       "add_shared_journal_tx" -> {
                           selectedSharedJournalId?.let { id ->
                               AddJournalTransactionScreen(
                                   journalId = id,
                                   viewModel = cloudViewModel,
                                   onBack = { activeForm = "journal_detail" }
                               )
                           }
                       }

                        "reminders" -> {
                            ReminderScreen(
                                viewModel = viewModel,
                                onDismiss = { activeForm = null }
                            )
                        }
                        "autopay" -> {
                            AutoPayScreen(
                                viewModel = viewModel,
                                onDismiss = { activeForm = null }
                            )
                        }
                        "navigation_customization" -> {
                            NavigationCustomizationScreen(
                                viewModel = viewModel,
                                onBack = { activeForm = null }
                            )
                        }
                    }
            }
        }

        val isProcessingFile by viewModel.isProcessingFile.collectAsState()
        val fileProcessingMessage by viewModel.fileProcessingMessage.collectAsState()

        if (isProcessingFile) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {}, // Consume all touch events to block user interaction
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp).testTag("file_processing_indicator")
                        )
                        Text(
                            text = fileProcessingMessage,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
