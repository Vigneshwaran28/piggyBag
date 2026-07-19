package com.titanbag.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.titanbag.app.data.*
import com.titanbag.app.ui.components.TitanBagMenuItem
import kotlinx.coroutines.launch
import java.util.Locale

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: TitanBagViewModel,
    cloudViewModel: CloudJournalViewModel,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onExportPdf: (String) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAddBudget: () -> Unit,
    onNavigateToAddRecurring: () -> Unit,
    onNavigateToCloudDashboard: () -> Unit,
    onNavigateToCloudPartner: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onNavigateToCloudLogin: () -> Unit,
    onNavigateToCloudShare: () -> Unit,
    onNavigateToFinanceDashboard: () -> Unit = {},
    onNavigateToPartnerSharing: () -> Unit = {},
    onNavigateToGroupExpenseSplit: () -> Unit = {},
    onNavigateToDebtList: () -> Unit = {},
    onNavigateToThemeCustomization: () -> Unit = {},
    onNavigateToHelpGuide: () -> Unit = {},
    onNavigateToGarage: () -> Unit = {},
    onNavigateToInvestments: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToAutoPay: () -> Unit = {},
    onNavigateToNavigationCustomization: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val recurringRules by viewModel.allRecurringTransactions.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var showBudgetListSheet by remember { mutableStateOf(false) }
    var showScheduledListSheet by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportOptions by remember { mutableStateOf(false) }
    var showImportOptions by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()

    val currencySymbol = settings?.currency ?: "₹"

    val activeUserId by viewModel.currentUserId.collectAsState()
    val localProfiles by viewModel.allLocalUserProfiles.collectAsState()
    val partnerConnections by viewModel.partnerConnections.collectAsState()
    
    val activeProfile = localProfiles.find { it.id == activeUserId }
    val connectedPartner = partnerConnections.find { it.status == "connected" || it.status == "linked" }
    val partnerProfile = localProfiles.find { it.id == connectedPartner?.partnerUserId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        com.titanbag.app.ui.components.TitanBagTopBar(title = "PiggyBag")


        // SECTION 0: ACCOUNT - Always first
        val cloudUser by cloudViewModel.currentUser.collectAsState(initial = null)
        Text("Account", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column {
                if (cloudUser == null) {
                    TitanBagMenuItem(
                        icon = Icons.Rounded.CloudQueue,
                        title = "Sign In",
                        subtitle = "Backup data and enable partner sharing",
                        onClick = onNavigateToCloudLogin
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCloudLogin() }
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            if (!cloudUser?.profilePhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = cloudUser?.profilePhoto,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cloudUser?.displayName ?: "Cloud User", fontWeight = FontWeight.Bold)
                            Text(cloudUser?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // LOCAL USER PROFILE SECTION - Only shown if partner is connected/linked
        if (connectedPartner != null) {
            Text("Active Profiles", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(activeProfile?.name?.take(1)?.uppercase() ?: "U", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(activeProfile?.name ?: "Current User", fontWeight = FontWeight.Bold)
                            Text("Primary Profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(partnerProfile?.name?.take(1)?.uppercase() ?: "P", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(partnerProfile?.name ?: "Partner", fontWeight = FontWeight.Bold)
                            Text("Linked Partner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // SECTION 1: FINANCIAL MANAGEMENT
        Text("Management Hub", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column {
                TitanBagMenuItem(
                    icon = Icons.Rounded.List,
                    title = "Transactions",
                    subtitle = "View and manage all records",
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBgColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    onClick = { viewModel.selectTab(0) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                TitanBagMenuItem(
                    icon = Icons.Rounded.Category,
                    title = "Categories",
                    subtitle = "Organize transactions",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    onClick = onNavigateToCategories
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                TitanBagMenuItem(
                    icon = Icons.Rounded.DonutLarge,
                    title = "Budgets",
                    subtitle = "Manage monthly limits",
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    onClick = { showBudgetListSheet = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                TitanBagMenuItem(
                    icon = Icons.Rounded.Update,
                    title = "AutoPay",
                    subtitle = "Manage recurring bills & rules",
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBgColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    onClick = onNavigateToAutoPay
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                TitanBagMenuItem(
                    icon = Icons.Rounded.People,
                    title = "Partner Sharing",
                    subtitle = "Share journals with your partner",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    onClick = {
                        if (cloudUser == null) {
                            viewModel.showSnackbar("Sign-in required")
                        } else {
                            onNavigateToPartnerSharing()
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                TitanBagMenuItem(
                    icon = Icons.Rounded.Group,
                    title = "Group Expense Split",
                    subtitle = "Split vacation expenses with friends",
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    onClick = {
                        if (cloudUser == null) {
                            viewModel.showSnackbar("Sign-in required")
                        } else {
                            onNavigateToGroupExpenseSplit()
                        }
                    }
                )
                if (settings?.debtListEnabled != false) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    TitanBagMenuItem(
                        icon = Icons.Rounded.ListAlt,
                        title = "Debt List",
                        subtitle = "Manage personal debts and credits",
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBgColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        onClick = onNavigateToDebtList
                    )
                }



                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                TitanBagMenuItem(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Smart Reminders",
                    subtitle = "Due notifications and recurring alerts",
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    onClick = onNavigateToReminders
                )
            }
        }

        // SECTION 2: PREFERENCES
        Text("Preferences", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToThemeCustomization() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Theme & Typography", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Customize theme palette, custom colors, RGB range and font styles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNavigationCustomization() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Customize Navigation Tabs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Pin and reorder up to 5 favorite modules to bottom bar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Debt Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Show debt list in management hub", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = settings?.debtListEnabled ?: true,
                        onCheckedChange = { viewModel.updateSettings(settings?.themeMode ?: "system", settings?.currency ?: "₹", settings?.notificationsEnabled ?: true, it) }
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Currency", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Select active coin visual symbol", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Button(
                        onClick = { showCurrencyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text(settings?.currency ?: "₹", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }


        // SECTION 3: DATA
        Text("Data Management", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column {
                TitanBagMenuItem(
                    icon = Icons.Rounded.Share,
                    title = "Export Data",
                    subtitle = "Export to CSV or PDF",
                    onClick = { showExportOptions = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                TitanBagMenuItem(
                    icon = Icons.Rounded.FileOpen,
                    title = "Import Data",
                    subtitle = "Import from CSV",
                    onClick = { showImportOptions = true }
                )
            }
        }

        // SECTION 4: HELP & GUIDE
        Text("Help & Guide", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            TitanBagMenuItem(
                icon = Icons.Rounded.HelpOutline,
                title = "Help Center",
                subtitle = "Read app guides, accounts, budgets and sync instructions",
                iconTint = MaterialTheme.colorScheme.primary,
                iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                onClick = onNavigateToHelpGuide
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showExportOptions) {
        ExportImportSheet(
            title = "Export Data", sheetState = exportSheetState, onDismiss = { showExportOptions = false },
            options = listOf(
                ExportImportOption("CSV Spreadsheet (.csv)", Icons.Rounded.TableChart, MaterialTheme.colorScheme.tertiary) { coroutineScope.launch { exportSheetState.hide(); showExportOptions = false; onExportCsv() } },
                ExportImportOption("PDF Document (.pdf)", Icons.Rounded.PictureAsPdf, MaterialTheme.colorScheme.secondary) { coroutineScope.launch { exportSheetState.hide(); showExportOptions = false; showPdfExportDialog = true } }
            )
        )
    }

    if (showImportOptions) {
        ExportImportSheet(
            title = "Import Data", sheetState = importSheetState, onDismiss = { showImportOptions = false },
            options = listOf(ExportImportOption("Import from CSV (.csv)", Icons.Rounded.TableChart, MaterialTheme.colorScheme.secondary) { coroutineScope.launch { importSheetState.hide(); showImportOptions = false; onImportCsv() } })
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = settings?.currency ?: "₹", onDismiss = { showCurrencyDialog = false },
            onSelect = { viewModel.updateSettings(settings?.themeMode ?: "system", it, settings?.notificationsEnabled ?: true); showCurrencyDialog = false }
        )
    }

    if (showBudgetListSheet) {
        BudgetListDialog(
            budgets = budgets,
            categories = categories,
            transactions = transactions,
            currencySymbol = currencySymbol,
            onDismiss = { showBudgetListSheet = false },
            onNavigateToAddBudget = onNavigateToAddBudget,
            onDeleteBudget = { viewModel.deleteBudget(it); viewModel.showSnackbar("Budget deleted") }
        )
    }

    if (showScheduledListSheet) {
        ScheduledTxnsDialog(
            recurringRules = recurringRules,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = { showScheduledListSheet = false },
            onNavigateToAddRecurring = onNavigateToAddRecurring,
            onDeleteRule = { viewModel.deleteRecurringRule(it); viewModel.showSnackbar("Scheduled Txn deleted") }
        )
    }

    if (showPdfExportDialog) {
        PdfExportOptionsDialog(
            viewModel = viewModel,
            onDismiss = { showPdfExportDialog = false },
            onExport = { filename ->
                showPdfExportDialog = false
                onExportPdf(filename)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportOptionsDialog(
    viewModel: TitanBagViewModel,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val context = LocalContext.current
    
    // States from view model
    val dateRange by viewModel.pdfDateRange.collectAsState()
    val customStart by viewModel.pdfCustomStartDate.collectAsState()
    val customEnd by viewModel.pdfCustomEndDate.collectAsState()
    val dateFormat by viewModel.pdfDateFormat.collectAsState()
    val timeFormat by viewModel.pdfTimeFormat.collectAsState()
    
    val incNotes by viewModel.pdfIncludeNotes.collectAsState()
    val incCategories by viewModel.pdfIncludeCategories.collectAsState()
    val incAccount by viewModel.pdfIncludeAccount.collectAsState()
    val incBalance by viewModel.pdfIncludeRunningBalance.collectAsState()
    val incSummary by viewModel.pdfIncludeSummary.collectAsState()
    val incIds by viewModel.pdfIncludeTransactionIds.collectAsState()

    var tempRange by remember { mutableStateOf(dateRange) }
    var tempStart by remember { mutableStateOf(customStart) }
    var tempEnd by remember { mutableStateOf(customEnd) }
    var tempDateFormat by remember { mutableStateOf(tempRange.let { dateFormat }) }
    var tempTimeFormat by remember { mutableStateOf(timeFormat) }
    
    var tempNotes by remember { mutableStateOf(incNotes) }
    var tempCategories by remember { mutableStateOf(incCategories) }
    var tempAccount by remember { mutableStateOf(incAccount) }
    var tempBalance by remember { mutableStateOf(incBalance) }
    var tempSummary by remember { mutableStateOf(incSummary) }
    var tempIds by remember { mutableStateOf(incIds) }

    val sdfDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF Export Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Date Range Selection
                Text("Date Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val ranges = listOf("Today", "Yesterday", "This Week", "This Month", "Last Month", "Custom")
                var rangeExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tempRange,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { rangeExpanded = true }) {
                                Icon(Icons.Rounded.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rangeExpanded = true }
                    )
                    DropdownMenu(expanded = rangeExpanded, onDismissRequest = { rangeExpanded = false }) {
                        ranges.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    tempRange = r
                                    rangeExpanded = false
                                }
                            )
                        }
                    }
                }

                // If Custom is selected, show Date Pickers
                if (tempRange == "Custom") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (tempStart > 0) sdfDisplay.format(Date(tempStart)) else "Start Date",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    if (tempStart > 0) cal.timeInMillis = tempStart
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance()
                                            c.set(y, m, d, 0, 0, 0)
                                            tempStart = c.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        )
                        OutlinedTextField(
                            value = if (tempEnd > 0) sdfDisplay.format(Date(tempEnd)) else "End Date",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    if (tempEnd > 0) cal.timeInMillis = tempEnd
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance()
                                            c.set(y, m, d, 23, 59, 59)
                                            tempEnd = c.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        )
                    }
                }

                // 2. Date Format
                Text("Date Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val formats = listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD", "DD MMM YYYY")
                var formatExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tempDateFormat,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { formatExpanded = true }) {
                                Icon(Icons.Rounded.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { formatExpanded = true }
                    )
                    DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                        formats.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = {
                                    tempDateFormat = f
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }

                // 3. Time Format
                Text("Time Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("12-hour", "24-hour").forEach { tf ->
                        FilterChip(
                            selected = tempTimeFormat == tf,
                            onClick = { tempTimeFormat = tf },
                            label = { Text(if (tf == "12-hour") "12-hour (08:45 PM)" else "24-hour (20:45)") }
                        )
                    }
                }

                // 4. Content Toggle Checkboxes
                Text("Content Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempNotes = !tempNotes },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempNotes, onCheckedChange = { tempNotes = it })
                        Text("Include notes", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempCategories = !tempCategories },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempCategories, onCheckedChange = { tempCategories = it })
                        Text("Include categories", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempAccount = !tempAccount },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempAccount, onCheckedChange = { tempAccount = it })
                        Text("Include account name", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempBalance = !tempBalance },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempBalance, onCheckedChange = { tempBalance = it })
                        Text("Include running balance", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempSummary = !tempSummary },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempSummary, onCheckedChange = { tempSummary = it })
                        Text("Include summary cards", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempIds = !tempIds },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = tempIds, onCheckedChange = { tempIds = it })
                        Text("Include transaction IDs", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updatePdfExportOptions(
                        range = tempRange,
                        start = tempStart,
                        end = tempEnd,
                        dateFormat = tempDateFormat,
                        timeFormat = tempTimeFormat,
                        incNotes = tempNotes,
                        incCats = tempCategories,
                        incAcc = tempAccount,
                        incBalance = tempBalance,
                        incSummary = tempSummary,
                        incIds = tempIds
                    )
                    val filename = getDynamicPdfFilename(tempRange, tempStart, tempEnd)
                    onExport(filename)
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getDynamicPdfFilename(
    range: String,
    customStart: Long,
    customEnd: Long
): String {
    val sdfDay = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val sdfMonth = SimpleDateFormat("MM_yyyy", Locale.getDefault())
    val sdfRangeSameMonth = SimpleDateFormat("dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    val today = Date()
    
    val (start, end) = when (range) {
        "Today" -> Pair(today, today)
        "Yesterday" -> {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yest = calendar.time
            Pair(yest, yest)
        }
        "This Week" -> {
            calendar.time = today
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val startW = calendar.time
            Pair(startW, today)
        }
        "This Month" -> {
            calendar.time = today
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startM = calendar.time
            Pair(startM, today)
        }
        "Last Month" -> {
            calendar.time = today
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startLM = calendar.time
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
            val endLM = calendar.time
            Pair(startLM, endLM)
        }
        "Custom" -> {
            if (customStart > 0 && customEnd > 0) {
                Pair(Date(customStart), Date(customEnd))
            } else {
                Pair(today, today)
            }
        }
        else -> Pair(today, today)
    }

    val calStart = Calendar.getInstance().apply { time = start }
    val calEnd = Calendar.getInstance().apply { time = end }

    val datePart = if (range == "This Month" || range == "Last Month") {
        sdfMonth.format(start)
    } else if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
               calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH) &&
               calStart.get(Calendar.DAY_OF_MONTH) == calEnd.get(Calendar.DAY_OF_MONTH)) {
        sdfDay.format(start)
    } else if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
               calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH)) {
        val startDay = sdfRangeSameMonth.format(start)
        val endDay = sdfRangeSameMonth.format(end)
        val monthYear = SimpleDateFormat("MM_yyyy", Locale.getDefault()).format(start)
        "${startDay}-${endDay}_${monthYear}"
    } else {
        val startStr = SimpleDateFormat("dd_MM", Locale.getDefault()).format(start)
        val endStr = SimpleDateFormat("dd_MM", Locale.getDefault()).format(end)
        val yearStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(end)
        "${startStr}-${endStr}_${yearStr}"
    }

    return "piggyBag_transaction_${datePart}.pdf"
}
