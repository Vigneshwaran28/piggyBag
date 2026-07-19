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
import com.titanbag.app.data.Investment
import com.titanbag.app.data.TitanBagViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    viewModel: TitanBagViewModel,
    onDismiss: () -> Unit
) {
    val investments by viewModel.allInvestments.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currency ?: "₹"

    var showForm by remember { mutableStateOf(false) }
    var investmentToEdit by remember { mutableStateOf<Investment?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    // Compute portfolio totals
    val totalInvested = investments.sumOf { it.purchasePrice * it.quantity + it.transactionCharges }
    val totalCurrentVal = investments.sumOf { it.currentPrice * it.quantity }
    val totalReturn = totalCurrentVal - totalInvested
    val returnPercent = if (totalInvested > 0.0) (totalReturn / totalInvested * 100) else 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Investments Portfolio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        investmentToEdit = null
                        showForm = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Investment")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (showForm) {
            InvestmentFormSheet(
                viewModel = viewModel,
                investment = investmentToEdit,
                onSave = { inv ->
                    if (inv.id == 0) {
                        viewModel.insertInvestment(
                            name = inv.name,
                            type = inv.type,
                            purchaseDate = inv.purchaseDate,
                            purchasePrice = inv.purchasePrice,
                            qty = inv.quantity,
                            broker = inv.broker,
                            charges = inv.transactionCharges,
                            notes = inv.notes
                        )
                    } else {
                        viewModel.updateInvestment(
                            id = inv.id,
                            name = inv.name,
                            type = inv.type,
                            purchaseDate = inv.purchaseDate,
                            purchasePrice = inv.purchasePrice,
                            qty = inv.quantity,
                            broker = inv.broker,
                            charges = inv.transactionCharges,
                            currentPrice = inv.currentPrice,
                            status = inv.currentStatus,
                            notes = inv.notes
                        )
                    }
                    showForm = false
                },
                onDelete = { inv ->
                    viewModel.deleteInvestment(inv)
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
                // Portfolio Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Net Asset Value (Portfolio)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$currencySymbol${String.format(Locale.US, "%,.2f", totalCurrentVal)}",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol${String.format(Locale.US, "%,.2f", totalInvested)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gains / Losses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (totalReturn >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (totalReturn >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${if (totalReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", returnPercent)}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (totalReturn >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Asset Holdings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (investments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No investment holdings tracked.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(investments) { inv ->
                            val invCost = inv.purchasePrice * inv.quantity + inv.transactionCharges
                            val invVal = inv.currentPrice * inv.quantity
                            val invReturn = invVal - invCost
                            val invReturnPct = if (invCost > 0.0) (invReturn / invCost * 100) else 0.0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        investmentToEdit = inv
                                        showForm = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Asset Icon mapping
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (inv.type.lowercase()) {
                                                    "gold" -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                                    "silver" -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                                    "crypto" -> Color(0xFFFF9900).copy(alpha = 0.2f)
                                                    else -> themeColor.copy(alpha = 0.2f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (inv.type.lowercase()) {
                                                "gold", "silver" -> Icons.Rounded.MonetizationOn
                                                "stocks", "mutual funds" -> Icons.Rounded.ShowChart
                                                "crypto" -> Icons.Rounded.CurrencyBitcoin
                                                else -> Icons.Rounded.AccountBalanceWallet
                                            },
                                            contentDescription = null,
                                            tint = when (inv.type.lowercase()) {
                                                "gold" -> Color(0xFFD4AF37)
                                                "silver" -> Color(0xFF8A95A5)
                                                "crypto" -> Color(0xFFFF9900)
                                                else -> themeColor
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(inv.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = "${inv.quantity} units @ $currencySymbol${inv.purchasePrice.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", invVal)}",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${if (invReturn >= 0) "+" else ""}${String.format(Locale.US, "%.1f", invReturnPct)}%",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (invReturn >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentFormSheet(
    viewModel: TitanBagViewModel,
    investment: Investment?,
    onSave: (Investment) -> Unit,
    onDelete: (Investment) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(investment?.name ?: "") }
    var type by remember { mutableStateOf(investment?.type ?: "Stocks") }
    var purchaseDate by remember { mutableStateOf(investment?.purchaseDate ?: "") }
    var purchasePriceStr by remember { mutableStateOf(investment?.purchasePrice?.toString() ?: "") }
    var qtyStr by remember { mutableStateOf(investment?.quantity?.toString() ?: "") }
    var broker by remember { mutableStateOf(investment?.broker ?: "") }
    var chargesStr by remember { mutableStateOf(investment?.transactionCharges?.toString() ?: "") }
    var currentPriceStr by remember { mutableStateOf(investment?.currentPrice?.toString() ?: "") }
    var status by remember { mutableStateOf(investment?.currentStatus ?: "Active") }
    var notes by remember { mutableStateOf(investment?.notes ?: "") }

    val isMetal = type.equals("Gold", ignoreCase = true) || type.equals("Silver", ignoreCase = true)

    // Trigger auto-fetching for gold/silver today's price
    LaunchedEffect(type) {
        if (isMetal && investment == null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            viewModel.fetchHistoricalPriceForInvestment(type, today) { rate ->
                currentPriceStr = String.format(Locale.US, "%.2f", rate)
                purchasePriceStr = String.format(Locale.US, "%.2f", rate)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (investment == null) "Add Investment" else "Edit Investment",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Asset Name (e.g. Reliance, HDFC Gold Fund)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Asset Type selector
        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text("Asset Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                listOf("Stocks", "Mutual Funds", "Gold", "Silver", "EPF/PPF", "Real Estate", "Crypto", "Other").forEach { item ->
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

        DatePickerField(label = "Purchase Date", date = purchaseDate, onDateSelected = { purchaseDate = it })

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = purchasePriceStr,
                onValueChange = { purchasePriceStr = it },
                label = { Text(if (isMetal) "Price per gram" else "Purchase Price") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            OutlinedTextField(
                value = qtyStr,
                onValueChange = { qtyStr = it },
                label = { Text(if (isMetal) "Weight (grams)" else "Quantity") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = broker,
                onValueChange = { broker = it },
                label = { Text("Broker/Platform") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = chargesStr,
                onValueChange = { chargesStr = it },
                label = { Text("Charges/Fees") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }

        OutlinedTextField(
            value = currentPriceStr,
            onValueChange = { currentPriceStr = it },
            label = { Text("Current Market Price") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

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
                        val purchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        val qty = qtyStr.toDoubleOrNull() ?: 1.0
                        val charges = chargesStr.toDoubleOrNull() ?: 0.0
                        val currentPrice = currentPriceStr.toDoubleOrNull() ?: purchasePrice
                        onSave(
                            Investment(
                                id = investment?.id ?: 0,
                                name = name,
                                type = type,
                                purchaseDate = purchaseDate.ifEmpty { "2026-01-01" },
                                purchasePrice = purchasePrice,
                                quantity = qty,
                                broker = broker.takeIf { it.isNotEmpty() },
                                transactionCharges = charges,
                                currentPrice = currentPrice,
                                currentStatus = status,
                                notes = notes.takeIf { it.isNotEmpty() },
                                userId = investment?.userId ?: ""
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

        if (investment != null) {
            TextButton(
                onClick = { onDelete(investment) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Investment")
            }
        }
    }
}
