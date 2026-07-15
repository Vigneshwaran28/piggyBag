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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.titanbag.app.data.*
import com.titanbag.app.ui.components.TitanBagMenuItem
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: TitanBagViewModel,
    cloudViewModel: CloudJournalViewModel,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onExportPdf: () -> Unit,
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
    onNavigateToDebtList: () -> Unit = {}
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
        
        com.titanbag.app.ui.components.TitanBagTopBar(title = "TitanBag")


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
                        subtitle = "Connect to TitanBag Cloud",
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
                    title = "Scheduled Transactions",
                    subtitle = "Manage recurring bills",
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBgColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    onClick = { showScheduledListSheet = true }
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
                            viewModel.showSnackbar("Cloud sign-in is required for Partner Sharing")
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
                            viewModel.showSnackbar("Cloud sign-in is required for Group Expense Split")
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
                ThemeModeSelector(settings, viewModel)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ThemePaletteSelection(settings, viewModel)
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

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showExportOptions) {
        ExportImportSheet(
            title = "Export Data", sheetState = exportSheetState, onDismiss = { showExportOptions = false },
            options = listOf(
                ExportImportOption("CSV Spreadsheet (.csv)", Icons.Rounded.TableChart, MaterialTheme.colorScheme.tertiary) { coroutineScope.launch { exportSheetState.hide(); showExportOptions = false; onExportCsv() } },
                ExportImportOption("PDF Document (.pdf)", Icons.Rounded.PictureAsPdf, MaterialTheme.colorScheme.secondary) { coroutineScope.launch { exportSheetState.hide(); showExportOptions = false; onExportPdf() } }
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
}
