package com.titanbag.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.titanbag.app.data.Settings
import com.titanbag.app.data.TitanBagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationCustomizationScreen(
    viewModel: TitanBagViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    
    val allModules = remember(settings?.debtListEnabled) {
        val list = mutableListOf(
            "Home", "Analytics", "Budgets", "Accounts", "Transactions", "Categories",
            "Partner Sharing", "Group Expenses", "AutoPay", "Investments", "Subscriptions"
        )
        if (settings?.debtListEnabled == true) {
            list.add(5, "Debt")
        }
        list
    }

    val defaultTabs = listOf("Home", "Analytics", "Budgets", "Accounts", "More")
    
    var isEditMode by remember { mutableStateOf(false) }

    // Selected tabs list
    val initialTabs = remember(settings) {
        settings?.bottomTabs?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: defaultTabs
    }
    
    val selectedTabs = remember { mutableStateListOf<String>() }

    LaunchedEffect(initialTabs) {
        selectedTabs.clear()
        // We omit "More" from selection as it must always be the 5th tab
        selectedTabs.addAll(initialTabs.filter { it != "More" }.take(4))
    }

    val hasChanges = remember(settings, selectedTabs) {
        val currentConfig = (selectedTabs + "More").joinToString(",")
        currentConfig != (settings?.bottomTabs ?: defaultTabs.joinToString(","))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Bottom Navigation Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditMode) {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Tabs")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Pin your favorite 5 screens to the bottom bar. The 5th tab is always 'More' for settings and options.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            // Current Config Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bottom Bar Live Preview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewTabs = selectedTabs + "More"
                        previewTabs.forEach { tab ->
                            val icon = when (tab) {
                                "Home" -> Icons.Rounded.Home
                                "Analytics" -> Icons.Rounded.BarChart
                                "Budgets" -> Icons.Rounded.DonutLarge
                                "Accounts" -> Icons.Rounded.AccountBalance
                                "Transactions" -> Icons.Rounded.List
                                "Categories" -> Icons.Rounded.Category
                                "Debt" -> Icons.Rounded.ListAlt
                                "Partner Sharing" -> Icons.Rounded.People
                                "Group Expenses" -> Icons.Rounded.Group
                                "AutoPay" -> Icons.Rounded.Update
                                "Investments" -> Icons.Rounded.ShowChart
                                "Subscriptions" -> Icons.Rounded.CardMembership
                                else -> Icons.Rounded.MoreHoriz
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Text(tab, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (!isEditMode) {
                // View Mode
                Text("Current Active Modules", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedTabs.forEachIndexed { index, tab ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text((index + 1).toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(tab, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                // Edit Mode
                Text("Select Primary Modules (Top 4)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Tap modules in the order you want them to appear from left to right.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allModules.forEach { module ->
                        val isChecked = selectedTabs.contains(module)
                        val orderIndex = selectedTabs.indexOf(module)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedTabs.remove(module)
                                    } else {
                                        if (selectedTabs.size >= 4) {
                                            Toast.makeText(context, "Maximum 4 tabs can be selected. The 5th is always More.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedTabs.add(module)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(module, fontWeight = FontWeight.Medium)
                                
                                if (isChecked) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text((orderIndex + 1).toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val newConfig = defaultTabs.joinToString(",")
                            viewModel.updateSettings(
                                themeMode = settings?.themeMode ?: "system",
                                currency = settings?.currency ?: "₹",
                                notificationsEnabled = settings?.notificationsEnabled ?: true,
                                debtListEnabled = settings?.debtListEnabled ?: false,
                                bottomTabs = newConfig
                            )
                            Toast.makeText(context, "Navigation reset to defaults", Toast.LENGTH_SHORT).show()
                            isEditMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset to Default")
                    }

                    Button(
                        onClick = {
                            if (selectedTabs.isEmpty()) {
                                Toast.makeText(context, "Please select at least 1 tab", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (!hasChanges) {
                                Toast.makeText(context, "No changes found.", Toast.LENGTH_SHORT).show()
                                isEditMode = false
                                return@Button
                            }

                            val newConfig = (selectedTabs + "More").joinToString(",")
                            viewModel.updateSettings(
                                themeMode = settings?.themeMode ?: "system",
                                currency = settings?.currency ?: "₹",
                                notificationsEnabled = settings?.notificationsEnabled ?: true,
                                debtListEnabled = settings?.debtListEnabled ?: false,
                                bottomTabs = newConfig
                            )
                            
                            Toast.makeText(context, "Changes saved successfully.", Toast.LENGTH_SHORT).show()
                            isEditMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}
