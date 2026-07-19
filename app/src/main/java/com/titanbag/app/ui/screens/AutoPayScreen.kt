package com.titanbag.app.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.Account
import com.titanbag.app.data.AutoPay
import com.titanbag.app.data.Category
import com.titanbag.app.data.TitanBagViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPayScreen(
    viewModel: TitanBagViewModel,
    onDismiss: () -> Unit
) {
    val autoPays by viewModel.allAutoPays.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currency ?: "₹"

    var selectedAutoPayForDetail by remember { mutableStateOf<AutoPay?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("AutoPay & Bill Cycles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add AutoPay")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Active AutoPay Rules",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            if (autoPays.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recurring payments set up yet. Tap '+' to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(autoPays) { ap ->
                        val cat = categories.find { it.id == ap.categoryId }
                        val acc = accounts.find { it.id == ap.accountId }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAutoPayForDetail = ap },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CardMembership,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ap.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${ap.frequency} • Next: ${ap.nextExecutionDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$currencySymbol${ap.amount.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (ap.automaticEntryEnabled) {
                                        Text("Auto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        AutoPayFormSheet(
            viewModel = viewModel,
            categories = categories,
            accounts = accounts,
            onDismiss = { showCreateSheet = false }
        )
    }

    if (selectedAutoPayForDetail != null) {
        AutoPayDetailsSheet(
            viewModel = viewModel,
            autoPay = selectedAutoPayForDetail!!,
            categories = categories,
            accounts = accounts,
            onDismiss = { selectedAutoPayForDetail = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPayFormSheet(
    viewModel: TitanBagViewModel,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Every 2 Months", "Every 3 Months", "Quarterly", "Half Yearly", "Yearly", "Custom")
    
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var selectedFrequency by remember { mutableStateOf("Monthly") }
    var customIntervalDays by remember { mutableStateOf("30") }
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var endDate by remember { mutableStateOf("") }
    var automaticEntry by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var accMenuExpanded by remember { mutableStateOf(false) }
    var freqMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New AutoPay Rule", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("AutoPay Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Picker
            ExposedDropdownMenuBox(
                expanded = catMenuExpanded,
                onExpandedChange = { catMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = catMenuExpanded,
                    onDismissRequest = { catMenuExpanded = false }
                ) {
                    categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            onClick = {
                                selectedCategory = c
                                catMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Account Picker
            ExposedDropdownMenuBox(
                expanded = accMenuExpanded,
                onExpandedChange = { accMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Billing Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = accMenuExpanded,
                    onDismissRequest = { accMenuExpanded = false }
                ) {
                    accounts.forEach { a ->
                        DropdownMenuItem(
                            text = { Text(a.name) },
                            onClick = {
                                selectedAccount = a
                                accMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Frequency Picker
            ExposedDropdownMenuBox(
                expanded = freqMenuExpanded,
                onExpandedChange = { freqMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedFrequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = freqMenuExpanded,
                    onDismissRequest = { freqMenuExpanded = false }
                ) {
                    frequencies.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f) },
                            onClick = {
                                selectedFrequency = f
                                freqMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedFrequency == "Custom") {
                OutlinedTextField(
                    value = customIntervalDays,
                    onValueChange = { customIntervalDays = it },
                    label = { Text("Custom Interval (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(context, { _, y, m, d ->
                                startDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Date (Optional)") },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(context, { _, y, m, d ->
                                endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Automatic Entry execution", fontWeight = FontWeight.SemiBold)
                Switch(checked = automaticEntry, onCheckedChange = { automaticEntry = it })
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val catId = selectedCategory?.id ?: 0
                    val accId = selectedAccount?.id ?: 0

                    if (name.isBlank() || amt <= 0.0 || catId == 0 || accId == 0) {
                        Toast.makeText(context, "Please fill out required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.insertAutoPay(
                        AutoPay(
                            name = name,
                            categoryId = catId,
                            amount = amt,
                            accountId = accId,
                            frequency = selectedFrequency,
                            customIntervalDays = if (selectedFrequency == "Custom") customIntervalDays.toIntOrNull() else null,
                            startDate = startDate,
                            endDate = if (endDate.isBlank()) null else endDate,
                            automaticEntryEnabled = automaticEntry,
                            notes = notes,
                            nextExecutionDate = startDate
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save AutoPay Rule")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPayDetailsSheet(
    viewModel: TitanBagViewModel,
    autoPay: AutoPay,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }

    // State bindings
    var name by remember(autoPay) { mutableStateOf(autoPay.name) }
    var amountStr by remember(autoPay) { mutableStateOf(autoPay.amount.toString()) }
    var selectedCategory by remember(autoPay) { mutableStateOf(categories.find { it.id == autoPay.categoryId }) }
    var selectedAccount by remember(autoPay) { mutableStateOf(accounts.find { it.id == autoPay.accountId }) }
    var selectedFrequency by remember(autoPay) { mutableStateOf(autoPay.frequency) }
    var customIntervalDays by remember(autoPay) { mutableStateOf(autoPay.customIntervalDays?.toString() ?: "30") }
    var startDate by remember(autoPay) { mutableStateOf(autoPay.startDate) }
    var endDate by remember(autoPay) { mutableStateOf(autoPay.endDate ?: "") }
    var automaticEntry by remember(autoPay) { mutableStateOf(autoPay.automaticEntryEnabled) }
    var notes by remember(autoPay) { mutableStateOf(autoPay.notes) }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var accMenuExpanded by remember { mutableStateOf(false) }
    var freqMenuExpanded by remember { mutableStateOf(false) }

    val hasChanges = remember(
        name, amountStr, selectedCategory, selectedAccount, selectedFrequency,
        customIntervalDays, startDate, endDate, automaticEntry, notes
    ) {
        name != autoPay.name ||
        (amountStr.toDoubleOrNull() ?: 0.0) != autoPay.amount ||
        selectedCategory?.id != autoPay.categoryId ||
        selectedAccount?.id != autoPay.accountId ||
        selectedFrequency != autoPay.frequency ||
        customIntervalDays.toIntOrNull() != autoPay.customIntervalDays ||
        startDate != autoPay.startDate ||
        endDate != (autoPay.endDate ?: "") ||
        automaticEntry != autoPay.automaticEntryEnabled ||
        notes != autoPay.notes
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit AutoPay" else "AutoPay Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (!isEditMode) {
                    IconButton(onClick = { isEditMode = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
            }

            if (!isEditMode) {
                // Read Only Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailItem("Rule Name", name)
                        DetailItem("Amount", amountStr)
                        DetailItem("Category", selectedCategory?.name ?: "Unknown")
                        DetailItem("Billing Account", selectedAccount?.name ?: "Unknown")
                        DetailItem("Frequency", selectedFrequency)
                        DetailItem("Start Date", startDate)
                        DetailItem("End Date", if (endDate.isEmpty()) "None" else endDate)
                        DetailItem("Automatic Entry", if (automaticEntry) "Enabled" else "Disabled")
                        DetailItem("Notes", if (notes.isEmpty()) "No notes" else notes)
                    }
                }
            } else {
                // Edit Mode Enabled
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("AutoPay Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Picker
                ExposedDropdownMenuBox(
                    expanded = catMenuExpanded,
                    onExpandedChange = { catMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedCategory = c
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Account Picker
                ExposedDropdownMenuBox(
                    expanded = accMenuExpanded,
                    onExpandedChange = { accMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Billing Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accMenuExpanded,
                        onDismissRequest = { accMenuExpanded = false }
                    ) {
                        accounts.forEach { a ->
                            DropdownMenuItem(
                                text = { Text(a.name) },
                                onClick = {
                                    selectedAccount = a
                                    accMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Frequency Picker
                ExposedDropdownMenuBox(
                    expanded = freqMenuExpanded,
                    onExpandedChange = { freqMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFrequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = freqMenuExpanded,
                        onDismissRequest = { freqMenuExpanded = false }
                    ) {
                        val frequencies = listOf("Daily", "Weekly", "Monthly", "Every 2 Months", "Every 3 Months", "Quarterly", "Half Yearly", "Yearly", "Custom")
                        frequencies.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = {
                                    selectedFrequency = f
                                    freqMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedFrequency == "Custom") {
                    OutlinedTextField(
                        value = customIntervalDays,
                        onValueChange = { customIntervalDays = it },
                        label = { Text("Custom Interval (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    startDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            }
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Automatic Entry Execution", fontWeight = FontWeight.SemiBold)
                    Switch(checked = automaticEntry, onCheckedChange = { automaticEntry = it })
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteAutoPay(autoPay)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }

                    Button(
                        onClick = {
                            if (!hasChanges) {
                                Toast.makeText(context, "No changes found.", Toast.LENGTH_SHORT).show()
                                isEditMode = false
                                return@Button
                            }

                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            val catId = selectedCategory?.id ?: 0
                            val accId = selectedAccount?.id ?: 0

                            if (name.isBlank() || amt <= 0.0 || catId == 0 || accId == 0) {
                                Toast.makeText(context, "Please fill out required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.updateAutoPay(
                                autoPay.copy(
                                    name = name,
                                    categoryId = catId,
                                    amount = amt,
                                    accountId = accId,
                                    frequency = selectedFrequency,
                                    customIntervalDays = if (selectedFrequency == "Custom") customIntervalDays.toIntOrNull() else null,
                                    startDate = startDate,
                                    endDate = if (endDate.isBlank()) null else endDate,
                                    automaticEntryEnabled = automaticEntry,
                                    notes = notes
                                )
                            )
                            Toast.makeText(context, "Changes saved successfully.", Toast.LENGTH_SHORT).show()
                            isEditMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}
