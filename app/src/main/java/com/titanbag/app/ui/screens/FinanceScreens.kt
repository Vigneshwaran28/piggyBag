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

            item {
                Text("Bank Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(accounts) { account ->
                BankAccountCard(account)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Bank Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            items(transactions.take(10)) { transaction ->
                BankTransactionItem(transaction)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
