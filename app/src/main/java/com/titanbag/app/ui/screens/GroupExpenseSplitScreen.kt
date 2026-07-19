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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupExpenseSplitScreen(
    viewModel: TitanBagViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userGroups by viewModel.userGroups.collectAsState()
    
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }

    if (selectedGroup != null) {
        val group = selectedGroup!!
        val members by viewModel.getGroupMembersFlow(group.id).collectAsState(initial = emptyList())
        val expenses by viewModel.getGroupExpensesFlow(group.id).collectAsState(initial = emptyList())
        
        GroupDashboardScreen(
            group = group,
            members = members,
            expenses = expenses,
            viewModel = viewModel,
            onBack = { selectedGroup = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Group Settlements Hub", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showJoinGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.GroupAdd, contentDescription = "Join Group")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentUserId by viewModel.currentUserId.collectAsState()
                val isLoggedIn = currentUserId != "default_user"

                if (!isLoggedIn) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Group creation is not allowed. You can only join existing groups using a 6-digit PIN.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Button(
                        onClick = { showJoinGroupDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join Group")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showJoinGroupDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Group")
                        }

                        Button(
                            onClick = { showCreateGroupDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Group")
                        }
                    }
                }

                Text(
                    "My Groups",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (userGroups.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "You are not a member of any group yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(userGroups) { group ->
                            val membersFlow by viewModel.getGroupMembersFlow(group.id).collectAsState(initial = emptyList())
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGroup = group },
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
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Flight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                group.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(containerColor = when(group.status) {
                                                "Completed" -> Color(0xFFCAFFBF)
                                                "Archived" -> Color.LightGray
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            }) {
                                                Text(group.status, fontSize = 9.sp)
                                            }
                                        }
                                        Text(
                                            "PIN: ${group.groupPin} • ${membersFlow.size} members",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create Group
    if (showCreateGroupDialog) {
        var groupTitle by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var destination by remember { mutableStateOf("") }
        var budgetStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Expense Group") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Enter a title and configuration for your group event.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Group Title *") },
                        placeholder = { Text("e.g. Goa Trip") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destination") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it },
                        label = { Text("Budget") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupTitle.isNotBlank()) {
                            val budget = budgetStr.toDoubleOrNull() ?: 0.0
                            viewModel.createGroupLocal(
                                title = groupTitle,
                                description = description,
                                destination = destination,
                                budget = budget
                            ) { success, pin ->
                                if (success) {
                                    Toast.makeText(context, "Group created with PIN $pin", Toast.LENGTH_LONG).show()
                                    showCreateGroupDialog = false
                                } else {
                                    Toast.makeText(context, pin ?: "Failed to create group", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = groupTitle.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Join Group
    if (showJoinGroupDialog) {
        var groupPinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinGroupDialog = false },
            title = { Text("Join Expense Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the 6-digit PIN of the group you wish to join.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = groupPinInput,
                        onValueChange = { if (it.length <= 6) groupPinInput = it.filter { c -> c.isDigit() } },
                        label = { Text("6-Digit Group PIN") },
                        placeholder = { Text("743812") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupPinInput.length == 6) {
                            viewModel.joinGroupLocal(groupPinInput) { success, result ->
                                Toast.makeText(context, if (success) "Joined group $result!" else result, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showJoinGroupDialog = false
                                }
                            }
                        }
                    },
                    enabled = groupPinInput.length == 6,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDashboardScreen(
    group: Group,
    members: List<GroupMember>,
    expenses: List<GroupExpenseWithMember>,
    viewModel: TitanBagViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeUserId by viewModel.currentUserId.collectAsState()
    
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<GroupExpenseWithMember?>(null) }
    
    val groupSettlements by viewModel.getSettlementsForGroupFlow(group.id).collectAsState(initial = emptyList())

    // Computations
    val totalExpense = expenses.sumOf { it.amount }
    val averageShare = if (members.isNotEmpty()) totalExpense / members.size else 0.0
    val myExpense = expenses.filter { it.userId == activeUserId }.sumOf { it.amount }

    // Settlement Optimization
    val plainExpenses = expenses.map { 
        GroupExpense(
            id = it.id, 
            groupId = it.groupId, 
            userId = it.userId, 
            amount = it.amount, 
            description = it.description, 
            expenseDate = it.expenseDate, 
            createdAt = it.createdAt,
            participantsIncluded = it.participantsIncluded,
            splitType = it.splitType,
            shares = it.shares
        ) 
    }
    val calculatedSettlements = remember(members, expenses) {
        viewModel.calculateGroupSettlements(members, plainExpenses)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(group.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Group PIN: ${group.groupPin} • ${group.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.outlineColor())
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (group.status == "Running") {
                        Button(
                            onClick = {
                                viewModel.finalizeGroup(group.id, members, plainExpenses)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCAFFBF), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Finalize")
                        }
                    } else if (group.status == "Completed") {
                        TextButton(
                            onClick = {
                                viewModel.reopenGroup(group.id)
                            }
                        ) {
                            Text("Reopen")
                        }
                        
                        Button(
                            onClick = {
                                viewModel.archiveGroup(group.id)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Archive")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (group.status == "Running") {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Dashboard
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text("₹${totalExpense.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("My Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text("₹${myExpense.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Average", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text("₹${averageShare.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Members Summary
            Text("Members Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    members.forEach { member ->
                        val memberSpent = expenses.filter { it.userId == member.userId }.sumOf { it.amount }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(member.displayName, fontWeight = FontWeight.Medium)
                            }
                            Text("Spent: ₹${memberSpent.toInt()}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Settlement Section
            if (group.status == "Completed") {
                Text("Finalized Settlements Payments", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF007700))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (groupSettlements.isEmpty()) {
                            Text(
                                "Everyone is settled! No payments generated.",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            groupSettlements.forEach { payment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(payment.fromUserName, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(payment.toUserName, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("₹${String.format(Locale.getDefault(), "%.2f", payment.amount)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        
                                        val isPaid = payment.status == "Paid"
                                        Checkbox(
                                            checked = isPaid,
                                            onCheckedChange = { checked ->
                                                viewModel.updateGroupSettlementStatus(payment.id, if (checked) "Paid" else "Pending")
                                            }
                                        )
                                        Text(if (isPaid) "Paid" else "Pending", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Settlement Optimization (Preview)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (calculatedSettlements.isEmpty()) {
                            Text(
                                "Everyone is settled! No transactions needed.",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            calculatedSettlements.forEach { payment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(payment.fromName, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(payment.toName, fontWeight = FontWeight.Bold)
                                    }
                                    Text("₹${String.format(Locale.getDefault(), "%.2f", payment.amount)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Expenses List
            Text("Trip Expenses", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            if (expenses.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No group expenses recorded yet.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    expenses.forEach { exp ->
                        val isMine = exp.userId == activeUserId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Paid by: ${exp.memberName} • Split: ${exp.splitType} • ${exp.expenseDate.substringBefore("T")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                    if (isMine && group.status == "Running") {
                                        IconButton(onClick = { expenseToEdit = exp }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteGroupExpenseLocal(GroupExpense(exp.id, exp.groupId, exp.userId, exp.amount, exp.description, exp.expenseDate, exp.createdAt))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddEditGroupExpenseDialog(
            groupId = group.id,
            members = members,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { amount, desc, date, splitType, participants, shares ->
                viewModel.addGroupExpenseLocal(
                    groupId = group.id,
                    amount = amount,
                    description = desc,
                    dateStr = date,
                    splitType = splitType,
                    participantsIncluded = participants,
                    shares = shares
                )
                showAddExpenseDialog = false
            }
        )
    }

    // Edit Expense Dialog
    if (expenseToEdit != null) {
        val exp = expenseToEdit!!
        AddEditGroupExpenseDialog(
            groupId = group.id,
            members = members,
            existingExpense = exp,
            onDismiss = { expenseToEdit = null },
            onConfirm = { amount, desc, date, splitType, participants, shares ->
                viewModel.updateGroupExpenseLocal(
                    GroupExpense(
                        id = exp.id,
                        groupId = exp.groupId,
                        userId = exp.userId,
                        amount = amount,
                        description = desc,
                        expenseDate = date,
                        createdAt = exp.createdAt,
                        splitType = splitType,
                        participantsIncluded = participants,
                        shares = shares
                    )
                )
                expenseToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGroupExpenseDialog(
    groupId: String,
    members: List<GroupMember>,
    existingExpense: GroupExpenseWithMember? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf(existingExpense?.amount?.toInt()?.toString() ?: "") }
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }
    
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var dateStr by remember { mutableStateOf(existingExpense?.expenseDate?.substringBefore("T") ?: todayStr) }

    // Advanced Splits State
    val splitTypes = listOf("Equal", "Percentage", "Custom Amount", "Shares", "Only Selected Members")
    var selectedSplitType by remember { mutableStateOf(existingExpense?.splitType ?: "Equal") }
    
    // Checked status list for each member
    val checkedMembers = remember { mutableStateMapOf<String, Boolean>() }
    // Split input values (percentages, custom amounts, shares) for each member
    val splitValues = remember { mutableStateMapOf<String, String>() }

    // Init state values
    LaunchedEffect(existingExpense, members) {
        members.forEach { m ->
            checkedMembers[m.userId] = true
            splitValues[m.userId] = ""
        }
        
        if (existingExpense != null) {
            val incl = existingExpense.participantsIncluded.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (incl.isNotEmpty()) {
                members.forEach { m ->
                    checkedMembers[m.userId] = incl.contains(m.userId)
                }
            }
            
            val shValues = existingExpense.shares.split(",").map { it.trim() }
            if (incl.isNotEmpty() && shValues.size == incl.size) {
                incl.forEachIndexed { idx, pId ->
                    splitValues[pId] = shValues.getOrNull(idx) ?: ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingExpense == null) "Add Shared Expense" else "Edit Shared Expense") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    placeholder = { Text("e.g. Dinner split") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )

                // Split type dropdown
                var splitMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = splitMenuExpanded,
                    onExpandedChange = { splitMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSplitType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Split Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = splitMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = splitMenuExpanded,
                        onDismissRequest = { splitMenuExpanded = false }
                    ) {
                        splitTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedSplitType = type
                                    splitMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Listing inputs for members
                Text("Split Details Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                members.forEach { member ->
                    val isChecked = checkedMembers[member.userId] ?: true
                    val currentVal = splitValues[member.userId] ?: ""
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    checkedMembers[member.userId] = checked
                                }
                            )
                            Text(member.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (isChecked && selectedSplitType in listOf("Percentage", "Custom Amount", "Shares")) {
                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { splitValues[member.userId] = it },
                                label = {
                                    Text(when(selectedSplitType) {
                                        "Percentage" -> "%"
                                        "Custom Amount" -> "₹"
                                        else -> "Shares"
                                    })
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(90.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0 || description.isBlank()) {
                        Toast.makeText(context, "Please fill out amount and description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Build participants list
                    val selectedList = members.filter { checkedMembers[it.userId] == true }
                    if (selectedList.isEmpty()) {
                        Toast.makeText(context, "Please select at least 1 participant", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val participantsStr = selectedList.joinToString(",") { it.userId }

                    // Validate custom values and build shares string
                    val sharesList = mutableListOf<String>()
                    var validationError: String? = null

                    when (selectedSplitType) {
                        "Percentage" -> {
                            var sum = 0.0
                            selectedList.forEach { m ->
                                val pct = splitValues[m.userId]?.toDoubleOrNull() ?: 0.0
                                sum += pct
                                sharesList.add(pct.toString())
                            }
                            if (Math.abs(sum - 100.0) > 0.1) {
                                validationError = "Percentages must sum to exactly 100% (Current sum: $sum%)"
                            }
                        }
                        "Custom Amount" -> {
                            var sum = 0.0
                            selectedList.forEach { m ->
                                val valAmt = splitValues[m.userId]?.toDoubleOrNull() ?: 0.0
                                sum += valAmt
                                sharesList.add(valAmt.toString())
                            }
                            if (Math.abs(sum - amt) > 0.1) {
                                validationError = "Custom amounts must sum to the total expense amount ₹$amt (Current sum: ₹$sum)"
                            }
                        }
                        "Shares" -> {
                            selectedList.forEach { m ->
                                val sh = splitValues[m.userId]?.toDoubleOrNull() ?: 1.0
                                sharesList.add(sh.toString())
                            }
                        }
                        else -> {
                            // Equal or Selected Members does not need individual share values saved
                        }
                    }

                    if (validationError != null) {
                        Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    val sharesStr = sharesList.joinToString(",")

                    onConfirm(amt, description, dateStr, selectedSplitType, participantsStr, sharesStr)
                },
                enabled = amount.isNotBlank() && description.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MaterialTheme.outlineColor() = MaterialTheme.colorScheme.outline
