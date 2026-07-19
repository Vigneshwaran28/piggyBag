package com.titanbag.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.titanbag.app.data.LocalUserProfile
import com.titanbag.app.data.PartnerConnection
import com.titanbag.app.data.TitanBagViewModel
import com.titanbag.app.data.CloudJournalViewModel
import com.titanbag.app.data.TransactionWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerSharingScreen(
    viewModel: TitanBagViewModel,
    cloudViewModel: CloudJournalViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeUserId by viewModel.currentUserId.collectAsState()
    val profiles by viewModel.allLocalUserProfiles.collectAsState()
    val connections by viewModel.partnerConnections.collectAsState()
    val activeProfile = profiles.find { it.id == activeUserId }

    val segments = remember { mutableStateListOf("", "", "", "", "") }
    val codeInput = remember(segments[0], segments[1], segments[2], segments[3], segments[4]) {
        segments.joinToString("-")
    }
    var isCodeRevealed by remember { mutableStateOf(false) }
    var viewingPartnerTransactionsProfile by remember { mutableStateOf<LocalUserProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partner Sharing Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (activeUserId == "default_user") {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                text = "Cloud Account Required",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Partner sharing requires a PiggyBag cloud account to synchronize transactions securely. Please sign in or register to enable partner sharing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Section 1: My Partner Code
                Text(
                    "My Share Profile",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Share your invitation code with your partner to let them view your expenses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )

                        val code = activeProfile?.partnerShareCode ?: "********"
                        val displayText = if (isCodeRevealed) code else "********"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            IconButton(
                                onClick = { isCodeRevealed = !isCodeRevealed },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCodeRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isCodeRevealed) "Hide Code" else "Reveal Code",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Partner Share Code", code)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Code")
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join my expense journal.\n\nPartner Code:\n$code"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share via"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Code")
                            }
                        }
                    }
                }

                // Section 2: Enter Partner Code
                Text(
                    "Connect Partner",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PartnerCodeSegmentedInput(segments = segments)

                        Button(
                            onClick = {
                                if (activeUserId != "default_user") {
                                    cloudViewModel.connectPartner(codeInput) { success, msg ->
                                        Toast.makeText(context, msg ?: "Result received", Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            segments[0] = ""
                                            segments[1] = ""
                                            segments[2] = ""
                                            segments[3] = ""
                                            segments[4] = ""
                                        }
                                    }
                                } else {
                                    viewModel.connectPartnerLocal(codeInput) { success, msg ->
                                        Toast.makeText(context, msg ?: "Result received", Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            segments[0] = ""
                                            segments[1] = ""
                                            segments[2] = ""
                                            segments[3] = ""
                                            segments[4] = ""
                                        }
                                    }
                                }
                            },
                            enabled = segments[0].length == 3 && segments[1].length == 4 && segments[2].length == 4 && segments[3].length == 4 && segments[4].length == 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Partner")
                        }
                    }
                }

                // Section 3: Connected Partners
                Text(
                    "Connected Partner",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (connections.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No partner connected. Enter their code to link journals.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    connections.forEach { conn ->
                        val partner = profiles.find { it.id == conn.partnerUserId }
                        if (partner != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color.Red)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Linked with ${partner.name}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Email: ${partner.email}", style = MaterialTheme.typography.bodyMedium)
                                        
                                        val dateFormatted = try {
                                            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                            parser.parse(conn.connectedDate)?.let { formatter.format(it) } ?: conn.connectedDate
                                        } catch (e: Exception) {
                                            conn.connectedDate
                                        }
                                        Text("Connected: $dateFormatted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { viewingPartnerTransactionsProfile = partner },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.MenuBook, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Shared Journal")
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.disconnectPartnerLocal(partner.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.LinkOff, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Disconnect")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Shared Journal Read Only Viewer overlay
            viewingPartnerTransactionsProfile?.let { partner ->
                val partnerTransactions by viewModel.getPartnerTransactions(partner.id).collectAsState(initial = emptyList())

                SharedJournalTransactionsDialog(
                    partnerName = partner.name,
                    transactions = partnerTransactions,
                    onDismiss = { viewingPartnerTransactionsProfile = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedJournalTransactionsDialog(
    partnerName: String,
    transactions: List<TransactionWithDetails>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("$partnerName's Journal (Read Only)", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        // Info alert
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "This view is read-only. You cannot edit, delete, or add records to this journal.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Summary Statistics
                        val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
                        val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        "₹${String.format(Locale.getDefault(), "%.2f", totalIncome)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        "₹${String.format(Locale.getDefault(), "%.2f", totalExpense)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF44336)
                                    )
                                }
                            }
                        }

                        // Transactions List
                        if (transactions.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No transaction records found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(transactions.size) { index ->
                                    val tx = transactions[index]
                                    val isExpense = tx.type == "expense"
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        Color(android.graphics.Color.parseColor(tx.categoryColor)).copy(alpha = 0.15f),
                                                        RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    tx.categoryName.take(1),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(android.graphics.Color.parseColor(tx.categoryColor))
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    tx.note.ifBlank { tx.categoryName },
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    "${tx.accountName} • ${tx.transactionDate.substringBefore("T")}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            Text(
                                                text = "${if (isExpense) "-" else "+"}₹${String.format(Locale.getDefault(), "%.2f", tx.amount)}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50)
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
    )
}

private fun formatPartnerCode(input: String): String {
    val clean = input.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    val sb = StringBuilder()
    for (i in clean.indices) {
        if (i > 0 && i % 3 == 0) {
            sb.append("-")
        }
        sb.append(clean[i])
    }
    return sb.toString()
}

@Composable
fun PartnerCodeSegmentedInput(
    segments: androidx.compose.runtime.snapshots.SnapshotStateList<String>
) {
    val focusRequesters = remember { List(5) { FocusRequester() } }
    val segmentLengths = listOf(3, 4, 4, 4, 4)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..4) {
            val maxLength = segmentLengths[i]
            val focusRequester = focusRequesters[i]
            
            Box(
                modifier = Modifier.weight(maxLength.toFloat()),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = segments[i],
                    onValueChange = { input ->
                        val filtered = input.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
                        if (filtered.length <= maxLength) {
                            segments[i] = filtered
                            if (filtered.length == maxLength && i < 4) {
                                focusRequesters[i + 1].requestFocus()
                            }
                        } else if (filtered.length > maxLength) {
                            var remaining = filtered
                            for (j in i..4) {
                                val spaceLeft = segmentLengths[j]
                                val chunk = remaining.take(spaceLeft)
                                segments[j] = chunk
                                remaining = remaining.drop(spaceLeft)
                                if (remaining.isEmpty()) {
                                    focusRequesters[j].requestFocus()
                                    break
                                }
                            }
                            if (remaining.isNotEmpty()) {
                                focusRequesters[4].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && 
                                keyEvent.key == androidx.compose.ui.input.key.Key.Backspace) {
                                if (segments[i].isEmpty() && i > 0) {
                                    segments[i - 1] = segments[i - 1].dropLast(1)
                                    focusRequesters[i - 1].requestFocus()
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false
                    )
                )
                
                if (segments[i].isEmpty()) {
                    Text(
                        text = "X".repeat(maxLength),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            if (i < 4) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
