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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.Reminder
import com.titanbag.app.data.TitanBagViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    viewModel: TitanBagViewModel,
    onDismiss: () -> Unit
) {
    val reminders by viewModel.allReminders.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currency ?: "₹"

    var showForm by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Smart Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        reminderToEdit = null
                        showForm = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Reminder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (showForm) {
            ReminderFormSheet(
                viewModel = viewModel,
                reminder = reminderToEdit,
                onSave = { rem ->
                    if (rem.id == 0) {
                        viewModel.insertReminder(
                            title = rem.title,
                            type = rem.type,
                            dueDate = rem.dueDate,
                            amount = rem.amount,
                            recurrence = rem.recurrence
                        )
                    } else {
                        viewModel.updateReminder(
                            id = rem.id,
                            title = rem.title,
                            type = rem.type,
                            dueDate = rem.dueDate,
                            amount = rem.amount,
                            recurrence = rem.recurrence,
                            enabled = rem.enabled
                        )
                    }
                    showForm = false
                },
                onDelete = { rem ->
                    viewModel.deleteReminder(rem)
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
                if (reminders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No billing or life reminders configured", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reminders) { reminder ->
                            val daysLeft = daysBetweenTodayAnd(reminder.dueDate)
                            val isOverdue = daysLeft != null && daysLeft < 0
                            val isDueSoon = daysLeft != null && daysLeft in 0..3

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        reminderToEdit = reminder
                                        showForm = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                    else if (isDueSoon) Color(0xFFFF9800).copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                                else themeColor.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (reminder.type.lowercase()) {
                                                "utility" -> Icons.Rounded.ElectricalServices
                                                "rent" -> Icons.Rounded.Home
                                                "credit card" -> Icons.Rounded.CreditCard
                                                "insurance" -> Icons.Rounded.Shield
                                                "school fees" -> Icons.Rounded.School
                                                else -> Icons.Rounded.NotificationsActive
                                            },
                                            contentDescription = null,
                                            tint = if (isOverdue) MaterialTheme.colorScheme.error else themeColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reminder.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = "Due: ${reminder.dueDate} (${reminder.recurrence})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (reminder.amount != null) {
                                            Text(
                                                text = "$currencySymbol${reminder.amount!!.toInt()}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Switch(
                                            checked = reminder.enabled,
                                            onCheckedChange = { isEnabled ->
                                                viewModel.updateReminder(
                                                    id = reminder.id,
                                                    title = reminder.title,
                                                    type = reminder.type,
                                                    dueDate = reminder.dueDate,
                                                    amount = reminder.amount,
                                                    recurrence = reminder.recurrence,
                                                    enabled = isEnabled
                                                )
                                            },
                                            modifier = Modifier.scale(0.8f)
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
}

// Help scale switch down
@Composable
fun Modifier.scale(scale: Float): Modifier = this.graphicsLayer(scaleX = scale, scaleY = scale)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormSheet(
    viewModel: TitanBagViewModel,
    reminder: Reminder?,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var type by remember { mutableStateOf(reminder?.type ?: "Utility") }
    var dueDate by remember { mutableStateOf(reminder?.dueDate ?: "") }
    var amountStr by remember { mutableStateOf(reminder?.amount?.toString() ?: "") }
    var recurrence by remember { mutableStateOf(reminder?.recurrence ?: "None") }
    var enabled by remember { mutableStateOf(reminder?.enabled ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (reminder == null) "Add Reminder" else "Edit Reminder",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Reminder Title (e.g. Electricity Bill, Dad's Meds)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Type dropdown
        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text("Reminder Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                listOf("Utility", "Credit Card", "Rent", "Insurance", "SIP", "School Fees", "Medicine", "Other").forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            type = item
                            typeExpanded = false
                        }
                    )
                }
            }
        }

        DatePickerField(label = "Due Date", date = dueDate, onDateSelected = { dueDate = it })

        OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text("Amount (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        // Recurrence dropdown
        var recExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = recExpanded,
            onExpandedChange = { recExpanded = !recExpanded }
        ) {
            OutlinedTextField(
                value = recurrence,
                onValueChange = {},
                readOnly = true,
                label = { Text("Recurrence") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = recExpanded,
                onDismissRequest = { recExpanded = false }
            ) {
                listOf("None", "Daily", "Weekly", "Monthly", "Yearly").forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            recurrence = item
                            recExpanded = false
                        }
                    )
                }
            }
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
                    if (title.isNotBlank()) {
                        val amount = amountStr.toDoubleOrNull()
                        onSave(
                            Reminder(
                                id = reminder?.id ?: 0,
                                title = title,
                                type = type,
                                dueDate = dueDate.ifEmpty { "2026-01-01" },
                                amount = amount,
                                recurrence = recurrence,
                                enabled = enabled,
                                userId = reminder?.userId ?: ""
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

        if (reminder != null) {
            TextButton(
                onClick = { onDelete(reminder) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Reminder")
            }
        }
    }
}
