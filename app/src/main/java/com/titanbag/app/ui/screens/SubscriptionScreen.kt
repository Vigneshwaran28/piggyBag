package com.titanbag.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.Subscription
import com.titanbag.app.data.TitanBagViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: TitanBagViewModel,
    onDismiss: () -> Unit
) {
    val subscriptions by viewModel.allSubscriptions.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currency ?: "₹"

    var showForm by remember { mutableStateOf(false) }
    var subToEdit by remember { mutableStateOf<Subscription?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    // Compute monthly budget impact
    var monthlyImpact = 0.0
    subscriptions.filter { it.status == "Active" }.forEach { sub ->
        when (sub.billingCycle) {
            "Weekly" -> monthlyImpact += sub.amount * 4.33
            "Monthly" -> monthlyImpact += sub.amount
            "Yearly" -> monthlyImpact += sub.amount / 12.0
            "Quarterly" -> monthlyImpact += sub.amount / 3.0
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions & Bills", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        subToEdit = null
                        showForm = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Subscription")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (showForm) {
            SubscriptionFormSheet(
                viewModel = viewModel,
                subscription = subToEdit,
                onSave = { sub ->
                    if (sub.id == 0) {
                        viewModel.insertSubscription(
                            name = sub.name,
                            amount = sub.amount,
                            cycle = sub.billingCycle,
                            start = sub.startDate,
                            nextRenewal = sub.nextRenewalDate,
                            accId = sub.accountId
                        )
                    } else {
                        viewModel.updateSubscription(
                            id = sub.id,
                            name = sub.name,
                            amount = sub.amount,
                            cycle = sub.billingCycle,
                            start = sub.startDate,
                            nextRenewal = sub.nextRenewalDate,
                            accId = sub.accountId,
                            status = sub.status
                        )
                    }
                    showForm = false
                },
                onDelete = { sub ->
                    viewModel.deleteSubscription(sub)
                    showForm = false
                },
                onDismiss = { showForm = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subscription Impact Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column {
                            Text("Monthly Subscription Impact", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currencySymbol${String.format(Locale.US, "%,.2f", monthlyImpact)} / month",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    "My Active Plans (${subscriptions.filter { it.status == "Active" }.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (subscriptions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active subscriptions logged yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(subscriptions) { sub ->
                            val daysLeft = daysBetweenTodayAnd(sub.nextRenewalDate)
                            val isDueSoon = daysLeft != null && daysLeft in 0..7

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        subToEdit = sub
                                        showForm = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDueSoon) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sub.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                            if (isDueSoon) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                    Text("Due in $daysLeft days", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Next Renewal: ${sub.nextRenewalDate} (${sub.billingCycle})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Text(
                                        text = "$currencySymbol${sub.amount.toInt()}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (sub.status == "Active") themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFormSheet(
    viewModel: TitanBagViewModel,
    subscription: Subscription?,
    onSave: (Subscription) -> Unit,
    onDelete: (Subscription) -> Unit,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.allAccounts.collectAsState()
    var name by remember { mutableStateOf(subscription?.name ?: "") }
    var amountStr by remember { mutableStateOf(subscription?.amount?.toString() ?: "") }
    var cycle by remember { mutableStateOf(subscription?.billingCycle ?: "Monthly") }
    var start by remember { mutableStateOf(subscription?.startDate ?: "") }
    var nextRenewal by remember { mutableStateOf(subscription?.nextRenewalDate ?: "") }
    var accId by remember { mutableStateOf(subscription?.accountId) }
    var status by remember { mutableStateOf(subscription?.status ?: "Active") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (subscription == null) "Add Subscription" else "Edit Subscription",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Subscription Name (e.g. Netflix, Gym, Office Rent)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text("Billing Amount") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        // Cycle Dropdown
        var cycleExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = cycleExpanded,
            onExpandedChange = { cycleExpanded = !cycleExpanded }
        ) {
            OutlinedTextField(
                value = cycle,
                onValueChange = {},
                readOnly = true,
                label = { Text("Billing Cycle") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = cycleExpanded,
                onDismissRequest = { cycleExpanded = false }
            ) {
                listOf("Weekly", "Monthly", "Quarterly", "Yearly").forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            cycle = item
                            cycleExpanded = false
                        }
                    )
                }
            }
        }

        DatePickerField(label = "Start Date", date = start, onDateSelected = { start = it })
        DatePickerField(label = "Next Renewal Date", date = nextRenewal, onDateSelected = { nextRenewal = it })

        // Linked Account selector dropdown
        var accountExpanded by remember { mutableStateOf(false) }
        val activeAccountName = accounts.find { it.id == accId }?.name ?: "No Linked Account"
        ExposedDropdownMenuBox(
            expanded = accountExpanded,
            onExpandedChange = { accountExpanded = !accountExpanded }
        ) {
            OutlinedTextField(
                value = activeAccountName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Link Payment Account") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = accountExpanded,
                onDismissRequest = { accountExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No Linked Account") },
                    onClick = {
                        accId = null
                        accountExpanded = false
                    }
                )
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name) },
                        onClick = {
                            accId = acc.id
                            accountExpanded = false
                        }
                    )
                }
            }
        }

        // Status Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subscription Status", fontWeight = FontWeight.Bold)
            Switch(
                checked = status == "Active",
                onCheckedChange = { status = if (it) "Active" else "Cancelled" }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        onSave(
                            Subscription(
                                id = subscription?.id ?: 0,
                                name = name,
                                amount = amount,
                                billingCycle = cycle,
                                startDate = start.ifEmpty { "2026-01-01" },
                                nextRenewalDate = nextRenewal.ifEmpty { "2026-02-01" },
                                accountId = accId,
                                status = status,
                                userId = subscription?.userId ?: ""
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }

        if (subscription != null) {
            TextButton(
                onClick = { onDelete(subscription) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Subscription")
            }
        }
    }
}
