package com.titanbag.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.BankAccount
import com.titanbag.app.data.BankTransaction
import com.titanbag.app.data.TitanBagViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboardScreen(
    viewModel: TitanBagViewModel,
    onBack: () -> Unit
) {
    val totalBalance by viewModel.totalBankBalance.collectAsState()
    val accounts by viewModel.allBankAccounts.collectAsState()
    val transactions by viewModel.allBankTransactions.collectAsState()
    val summary by viewModel.bankFinancialSummary.collectAsState()

    var selectedBankFilter by remember { mutableStateOf("All Banks") }
    var selectedTypeFilter by remember { mutableStateOf("All Types") }

    val filteredTransactions = transactions.filter {
        (selectedBankFilter == "All Banks" || it.bankName == selectedBankFilter) &&
        (selectedTypeFilter == "All Types" || it.type == selectedTypeFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TotalBalanceCard(totalBalance)
            }

            item {
                FinancialSummaryRow(summary.first, summary.second)
            }

            if (accounts.isNotEmpty()) {
                item {
                    FinancialInsightsCard(accounts, transactions)
                }
            }

            item {
                Text("Bank Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(accounts) { account ->
                BankAccountCard(account)
            }

            item {
                Column {
                    Text("Bank Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTypeFilter == "CREDIT",
                            onClick = { selectedTypeFilter = if (selectedTypeFilter == "CREDIT") "All Types" else "CREDIT" },
                            label = { Text("Credits") }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "DEBIT",
                            onClick = { selectedTypeFilter = if (selectedTypeFilter == "DEBIT") "All Types" else "DEBIT" },
                            label = { Text("Debits") }
                        )
                    }
                }
            }

            items(filteredTransactions) { transaction ->
                BankTransactionItem(transaction)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FinancialInsightsCard(accounts: List<BankAccount>, transactions: List<BankTransaction>) {
    val highestSpendingBank = transactions.filter { it.type == "DEBIT" }
        .groupBy { it.bankName }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .maxByOrNull { it.value }?.key ?: "N/A"

    val mostActiveAccount = transactions.groupBy { it.accountLastFour }
        .maxByOrNull { it.value.size }?.let { entry ->
            val bank = transactions.find { it.accountLastFour == entry.key }?.bankName ?: "Bank"
            "$bank (..${entry.key})"
        } ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Financial Insights", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            
            InsightRow("Highest Spending Bank", highestSpendingBank)
            InsightRow("Most Active Account", mostActiveAccount)
            InsightRow("Active Bank Accounts", accounts.size.toString())
        }
    }
}

@Composable
fun InsightRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TotalBalanceCard(balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Bank Balance", style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun FinancialSummaryRow(credits: Double, debits: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMiniCard(
            label = "Total Credits",
            amount = credits,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        SummaryMiniCard(
            label = "Total Debits",
            amount = debits,
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryMiniCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun BankAccountCard(account: BankAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.bankName, fontWeight = FontWeight.Bold)
                Text("A/c XXXX${account.accountLastFour}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                formatCurrency(account.currentBalance),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun BankTransactionItem(transaction: BankTransaction) {
    val isCredit = transaction.type == "CREDIT"
    val color = if (isCredit) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.bankName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(transaction.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text(transaction.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = (if (isCredit) "+" else "-") + formatCurrency(transaction.amount),
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}
