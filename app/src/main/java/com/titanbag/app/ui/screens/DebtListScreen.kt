package com.titanbag.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.titanbag.app.data.DebtRecord
import com.titanbag.app.data.TitanBagViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    viewModel: TitanBagViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val debtRecords by viewModel.allDebtRecords.collectAsState()
    
    var filterStatus by remember { mutableStateOf("All") } // "All", "Pending", "Completed"
    var showAddDialog by remember { mutableStateOf(false) }

    // Filter list
    val filteredList = remember(debtRecords, filterStatus) {
        when (filterStatus) {
            "Pending" -> debtRecords.filter { it.status.lowercase() == "pending" }
            "Completed" -> debtRecords.filter { it.status.lowercase() == "completed" }
            else -> debtRecords
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt & Credit Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Record")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Row
            val totalOwedToMe = debtRecords.filter { it.action.lowercase() == "credit" && it.status.lowercase() == "pending" }.sumOf { it.amount }
            val totalIOwe = debtRecords.filter { it.action.lowercase() == "debt" && it.status.lowercase() == "pending" }.sumOf { it.amount }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Light Green
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Owed to Me", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))
                        Text("₹${formatAmountWithCommas(totalOwedToMe)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Light Red
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("I Owe Others", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC62828))
                        Text("₹${formatAmountWithCommas(totalIOwe)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }

            // Tab Filter
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Pending", "Completed").forEach { tab ->
                    val isSelected = filterStatus == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { filterStatus = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // List of Records
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No records found under $filterStatus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                var expandedRecordId by remember { mutableStateOf<String?>(null) }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { record ->
                        val isExpanded = expandedRecordId == record.id
                        DebtRecordRow(
                            record = record,
                            isExpanded = isExpanded,
                            onToggleExpand = { expandedRecordId = if (isExpanded) null else record.id },
                            onMarkCompleted = { returnedDate ->
                                viewModel.updateDebtRecordLocal(
                                    record.copy(
                                        status = "Completed",
                                        returnedDate = returnedDate,
                                        remainderBoolean = false
                                    )
                                )
                                Toast.makeText(context, "Marked as returned", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteDebtRecordLocal(record)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Record Dialog
    if (showAddDialog) {
        AddDebtRecordDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, action, date, reminder, reminderTs, mode ->
                viewModel.insertDebtRecordLocal(name, amount, action, date, reminder, reminderTs, mode)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DebtRecordRow(
    record: DebtRecord,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMarkCompleted: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isCredit = record.action.lowercase() == "credit"
    val isPending = record.status.lowercase() == "pending"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        record.personName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            record.borrowedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (record.remainderBoolean && isPending && record.dateTimestamp != null) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = "Reminder enabled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "₹${formatAmountWithCommas(record.amount)}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        
                        // Badges Row
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Debt/Credit Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCredit) "Lent" else "Borrowed",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            
                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isPending) Color(0xFFFFF3E0) else Color(0xFFECEFF1),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = record.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPending) Color(0xFFEF6C00) else Color(0xFF37474F)
                                )
                            }
                        }
                    }
                }
            }

            // Accordion expanded area
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transaction Mode:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(record.modeOfTransaction, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    if (record.remainderBoolean && isPending && record.dateTimestamp != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Scheduled Alert:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            val displayTime = record.dateTimestamp.replace("T", " ")
                            Text(displayTime, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!record.returnedDate.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Returned Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(record.returnedDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPending) {
                            val calendar = Calendar.getInstance()
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val formattedDate = String.format("%d-%02d-%02d", year, month + 1, day)
                                    onMarkCompleted(formattedDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )

                            Button(
                                onClick = { datePickerDialog.show() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Returned")
                            }
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Record")
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Record", fontWeight = FontWeight.Bold) },
                                text = { Text("Are you sure you want to permanently delete this debt record?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm = false
                                            onDelete()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Boolean, String?, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("Debt") } // "Debt" or "Credit"
    var modeOfTransaction by remember { mutableStateOf("Cash") } // "Cash", "Card", "UPI", "Bank Transfer"
    
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var borrowedDate by remember { mutableStateOf(today) }

    var isReminderEnabled by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(today) }
    var reminderTime by remember { mutableStateOf("09:00") }

    val calendar = Calendar.getInstance()

    // Date Picker for Borrowed Date
    val borrowedDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            borrowedDate = String.format("%d-%02d-%02d", year, month + 1, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Date Picker for Reminder Date
    val reminderDatePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            reminderDate = String.format("%d-%02d-%02d", year, month + 1, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Time Picker for Reminder Time
    val reminderTimePicker = TimePickerDialog(
        context,
        { _, hour, minute ->
            reminderTime = String.format("%02d:%02d", hour, minute)
        },
        9, 0, true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Debt/Credit Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown: Debt / Credit selection
                var isActionDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = isActionDropdownExpanded,
                    onExpandedChange = { isActionDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (action == "Debt") "Borrowed (Debt)" else "Lent (Credit)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isActionDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isActionDropdownExpanded,
                        onDismissRequest = { isActionDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Borrowed (Debt)") },
                            onClick = {
                                action = "Debt"
                                isActionDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lent (Credit)") },
                            onClick = {
                                action = "Credit"
                                isActionDropdownExpanded = false
                            }
                        )
                    }
                }

                // Dropdown: Transaction Mode
                var isModeDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = isModeDropdownExpanded,
                    onExpandedChange = { isModeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = modeOfTransaction,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transaction Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModeDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isModeDropdownExpanded,
                        onDismissRequest = { isModeDropdownExpanded = false }
                    ) {
                        listOf("Cash", "Card", "UPI", "Bank Transfer").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode) },
                                onClick = {
                                    modeOfTransaction = mode
                                    isModeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker field
                OutlinedTextField(
                    value = borrowedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Borrowed/Lent Date") },
                    trailingIcon = {
                        IconButton(onClick = { borrowedDatePicker.show() }) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Reminder switch toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Schedule Reminder Alert", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it }
                    )
                }

                if (isReminderEnabled) {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = reminderDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alert Date") },
                            trailingIcon = {
                                IconButton(onClick = { reminderDatePicker.show() }) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = "Select Date")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alert Time") },
                            trailingIcon = {
                                IconButton(onClick = { reminderTimePicker.show() }) {
                                    Icon(Icons.Rounded.AccessTime, contentDescription = "Select Time")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (name.isNotBlank() && amountVal != null && amountVal > 0) {
                        val reminderTs = if (isReminderEnabled) "${reminderDate}T${reminderTime}" else null
                        onConfirm(name, amountVal, action, borrowedDate, isReminderEnabled, reminderTs, modeOfTransaction)
                    } else {
                        Toast.makeText(context, "Please enter valid fields", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatAmountWithCommas(amount: Double): String {
    try {
        val longVal = amount.toLong()
        val str = longVal.toString()
        if (str.length <= 3) return str
        
        val lastThree = str.substring(str.length - 3)
        val rest = str.substring(0, str.length - 3)
        val sb = java.lang.StringBuilder()
        
        var i = rest.length
        while (i > 0) {
            if (i >= 2) {
                sb.insert(0, rest.substring(i - 2, i))
                if (i > 2) sb.insert(0, ",")
                i -= 2
            } else {
                sb.insert(0, rest.substring(0, i))
                if (i > 0) sb.insert(0, ",")
                break
            }
        }
        var result = sb.toString()
        if (result.startsWith(",")) {
            result = result.substring(1)
        }
        return "$result,$lastThree"
    } catch (e: Exception) {
        return String.format("%,.0f", amount)
    }
}
