package com.titanbag.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.titanbag.app.data.*
import com.titanbag.app.ui.components.IconMapper
import com.titanbag.app.ui.components.AnimatedEntranceItem
import com.titanbag.app.ui.theme.AppTheme
import com.titanbag.app.ui.theme.getFriendlyName
import java.util.Locale

@Composable
fun ThemeModeSelector(settings: Settings?, viewModel: TitanBagViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Appearance Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Adjust dark or light theme preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentMode = settings?.themeMode ?: "system"
                val modes = listOf(
                    "system" to "SYSTEM",
                    "light" to "LIGHT",
                    "soft_dark" to "L-DARK",
                    "dark" to "DARK",
                    "pure_black" to "OLED"
                )
                modes.forEach { (mode, label) ->
                    val isSelected = currentMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                viewModel.updateSettings(mode, settings?.currency ?: "₹", settings?.notificationsEnabled ?: true, settings?.debtListEnabled ?: true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePaletteSelection(settings: Settings?, viewModel: TitanBagViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Theme Color Palette", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("Select your preferred color scheme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currentPalette = settings?.colorPalette
            val activePalette = when (currentPalette) {
                "Cosmic Slate", null -> "Default"
                else -> currentPalette
            }
            
            val palettes = listOf(
                Triple("Default", Color(0xFF005FAF), Color(0xFF60A5FA))
            ) + AppTheme.entries.map { 
                Triple(it.getFriendlyName(), it.primaryLight, it.primaryDark)
            }
            
            palettes.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (paletteName, lightColor, darkColor) ->
                        val isSelected = activePalette == paletteName
                        
                        Surface(
                            selected = isSelected,
                            onClick = { viewModel.updateColorPalette(paletteName) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape)) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).background(lightColor).align(Alignment.CenterStart))
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).background(darkColor).align(Alignment.CenterEnd))
                                }
                                Text(
                                    text = paletteName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (rowItems.size < 4) {
                        repeat(4 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val customColors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
            "#FFFFFF", "#000000"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Manual Primary/Icon Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            customColors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = settings?.colorPalette == "Custom" && settings?.customIconColor == hex
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.LightGray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            if (hex == settings?.customBgColor) {
                                android.widget.Toast.makeText(context, "Icon color cannot be the same as background color!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateCustomColors(hex, settings?.customBgColor)
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Manual Card/Background Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            customColors.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = settings?.colorPalette == "Custom" && settings?.customBgColor == hex
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.LightGray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            if (hex == settings?.customIconColor) {
                                android.widget.Toast.makeText(context, "Background color cannot be the same as icon color!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateCustomColors(settings?.customIconColor, hex)
                            }
                        }
                )
            }
        }
    }
}

data class ExportImportOption(val title: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportSheet(title: String, sheetState: SheetState, onDismiss: () -> Unit, options: List<ExportImportOption>) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            options.forEach { option ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = option.onClick,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(option.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = option.icon, contentDescription = null, tint = option.color)
                        }
                        Text(option.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencySelectionDialog(currentCurrency: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 500.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Default Currency", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 16.dp))
                var query by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search currency...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = CircleShape, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                val options = listOf(Pair("$", "USD"), Pair("₹", "INR"), Pair("€", "EUR"), Pair("£", "GBP"), Pair("¥", "JPY"))
                val filtered = options.filter { it.first.contains(query, true) || it.second.contains(query, true) }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { option ->
                        val isSelected = currentCurrency == option.first
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable { onSelect(option.first) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text(option.first, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option.second, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

@Composable
fun BudgetListDialog(
    budgets: List<Budget>,
    categories: List<Category>,
    transactions: List<TransactionWithDetails>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onNavigateToAddBudget: () -> Unit,
    onDeleteBudget: (Budget) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        com.titanbag.app.ui.components.TitanBagTopBar(
                            title = "Configured Budgets",
                            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onNavigationClick = onDismiss,
                            actionIcon = Icons.Rounded.Add,
                            onActionClick = { onNavigateToAddBudget(); onDismiss() }
                        )
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 100.dp, top = innerPadding.calculateTopPadding() + 12.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (budgets.isEmpty()) {
                        item { EmptyListPlaceholder("No Budgets Configured", "Set limits to track your spending", Icons.Rounded.DonutLarge) }
                    }
                    itemsIndexed(budgets) { idx, budget ->
                        val cat = categories.firstOrNull { it.id == budget.categoryId }
                        val label = budget.budgetName?.takeIf { it.isNotBlank() } ?: cat?.name ?: "Overall"
                        val color = try { Color(android.graphics.Color.parseColor(cat?.color ?: "#009688")) } catch (e: Exception) { Color.Gray }
                        val spent = transactions.filter { isTransactionInBudget(it, budget) }.sumOf { it.amount }
                        val progress = if (budget.budgetAmount > 0) (spent / budget.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        
                        AnimatedEntranceItem(index = idx) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                                IconMapper.CategoryIcon(icon = cat?.icon ?: "trending_down", categoryName = cat?.name ?: "", tint = Color.Black, modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(label, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                                Text("Limit: $currencySymbol${budget.budgetAmount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        IconButton(onClick = { onDeleteBudget(budget) }, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (spent > budget.budgetAmount) MaterialTheme.colorScheme.error else color)
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Spent: $currencySymbol${String.format(Locale.getDefault(), "%.1f", spent)}", style = MaterialTheme.typography.bodySmall)
                                        Text("Remaining: $currencySymbol${String.format(Locale.getDefault(), "%.1f", (budget.budgetAmount - spent).coerceAtLeast(0.0))}", style = MaterialTheme.typography.bodySmall)
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

@Composable
fun ScheduledTxnsDialog(
    recurringRules: List<RecurringTransaction>,
    categories: List<Category>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onNavigateToAddRecurring: () -> Unit,
    onDeleteRule: (RecurringTransaction) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        com.titanbag.app.ui.components.TitanBagTopBar(
                            title = "Scheduled Txns",
                            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onNavigationClick = onDismiss,
                            actionIcon = Icons.Rounded.Add,
                            onActionClick = { onNavigateToAddRecurring(); onDismiss() }
                        )
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 100.dp, top = innerPadding.calculateTopPadding() + 12.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recurringRules.isEmpty()) {
                        item { EmptyListPlaceholder("No Scheduled Txns", "Automate your repeating expenses", Icons.Rounded.Update) }
                    }
                    itemsIndexed(recurringRules) { idx, rule ->
                        val cat = categories.firstOrNull { it.id == rule.categoryId }
                        val color = try { Color(android.graphics.Color.parseColor(cat?.color ?: "#60A5FA")) } catch (e: Exception) { Color.Gray }
                        val isIncome = rule.type == "income"

                        AnimatedEntranceItem(index = idx) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                                IconMapper.CategoryIcon(icon = cat?.icon ?: "trending_up", categoryName = cat?.name ?: "", tint = Color.Black, modifier = Modifier.size(24.dp))
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(rule.note.ifEmpty { cat?.name ?: "Scheduled" }, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                                Text("${rule.frequency.uppercase(Locale.getDefault())} • Next: ${rule.nextExecutionDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        IconButton(onClick = { onDeleteRule(rule) }, modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth().background(if (isIncome) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isIncome) "Income" else "Expense", color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        Text("${if (isIncome) "+" else "-"}$currencySymbol${String.format(Locale.getDefault(), "%.1f", rule.amount)}", color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
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

@Composable
fun EmptyListPlaceholder(title: String, subtitle: String, icon: ImageVector) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}
