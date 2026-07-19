package com.titanbag.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.*
import com.titanbag.app.ui.components.*
import com.titanbag.app.ui.theme.LocalVisualStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: TitanBagViewModel,
    onSearchClick: () -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val themeColor = MaterialTheme.colorScheme.primary

    val accounts by viewModel.allAccounts.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val allTags by viewModel.allTags.collectAsState(initial = emptySet())
    val activeFilterType by viewModel.filterType.collectAsState()
    val activeFilterCategories by viewModel.filterCategoryIds.collectAsState()
    val activeFilterAccounts by viewModel.filterAccountIds.collectAsState()
    val activeFilterTags by viewModel.filterTags.collectAsState()
    val activeHomeDateFilter by viewModel.selectedHomeDateFilter.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDatePeriod by remember { mutableStateOf("All Time") }
    var showCustomDateRangeDialog by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val currency = settings?.currency ?: "₹"

    var localFilterType by remember(activeFilterType) {
        mutableStateOf(if (activeFilterType == "All") "expense" else activeFilterType)
    }

    LaunchedEffect(activeHomeDateFilter, currentMonthCalendar, customStartDate, customEndDate) {
        val startCal = (currentMonthCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (currentMonthCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        when (activeHomeDateFilter) {
            "Daily" -> {
                viewModel.filterDateRange.value = Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            "Weekly" -> {
                val firstDayOfWeek = Calendar.MONDAY
                startCal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                val endOfWeekCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                viewModel.filterDateRange.value = Pair(startCal.timeInMillis, endOfWeekCal.timeInMillis)
            }
            "Monthly" -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                viewModel.filterDateRange.value = Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            "6 Months" -> {
                startCal.add(Calendar.MONTH, -5)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                viewModel.filterDateRange.value = Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            "Yearly" -> {
                startCal.set(Calendar.MONTH, Calendar.JANUARY)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                viewModel.filterDateRange.value = Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
            "Custom" -> {
                if (customStartDate != null && customEndDate != null) {
                    val calEnd = Calendar.getInstance().apply {
                        timeInMillis = customEndDate!!
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    viewModel.filterDateRange.value = Pair(customStartDate, calEnd.timeInMillis)
                } else {
                    viewModel.filterDateRange.value = Pair(startCal.timeInMillis, endCal.timeInMillis)
                }
            }
        }
    }

    // Calculations
    val (incomeSum, expenseSum, netBalance) = remember(transactions) {
        val inc = transactions.filter { it.type == "income" }.sumOf { it.amount }.toFloat()
        val exp = transactions.filter { it.type == "expense" }.sumOf { it.amount }.toFloat()
        Triple(inc, exp, inc - exp)
    }

    // Group transactions by category for Pie Chart based on local filter type
    val categorySegments = remember(transactions, localFilterType) {
        val displayType = if (localFilterType == "income") "income" else "expense"
        val filtered = transactions.filter { it.type == displayType }
        val categoryTotals = filtered.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

        categoryTotals.map { (catId, total) ->
            val sampleTx = filtered.firstOrNull { it.categoryId == catId }
            val name = sampleTx?.categoryName ?: "Other"
            val colorStr = sampleTx?.categoryColor ?: "#9E9E9E"
            val color = try { Color(android.graphics.Color.parseColor(colorStr)) } catch (e: Exception) { Color.Gray }
            PieSegment(value = total, color = color, name = name)
        }
    }

    val topSpendingCategory = remember(categorySegments) {
        categorySegments.maxByOrNull { it.value }?.name ?: "N/A"
    }

    // Historical trends for Line Chart (net balances)
    val trendPoints = remember(transactions) {
        val sorted = transactions.sortedBy { it.transactionDate }
        var balanceAccumulator = 0f
        sorted.map { tx ->
            if (tx.type == "income") balanceAccumulator += tx.amount.toFloat()
            else balanceAccumulator -= tx.amount.toFloat()
            balanceAccumulator
        }
    }

    val visualStyle = LocalVisualStyle.current
    val isDiary = visualStyle == "diary"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .padding(horizontal = 16.dp)
    ) {
        // App Bar
        com.titanbag.app.ui.components.TitanBagTopBar(
            title = "PiggyBag",
            actionIcon = Icons.Rounded.Search,
            onActionClick = onSearchClick,
            actionContentDescription = "Search"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeHomeDateFilter != "Custom") {
                    IconButton(
                        onClick = {
                            currentMonthCalendar = (currentMonthCalendar.clone() as Calendar).apply {
                                when (activeHomeDateFilter) {
                                    "Daily" -> add(Calendar.DAY_OF_YEAR, -1)
                                    "Weekly" -> add(Calendar.WEEK_OF_YEAR, -1)
                                    "Monthly" -> add(Calendar.MONTH, -1)
                                    "6 Months" -> add(Calendar.MONTH, -6)
                                    "Yearly" -> add(Calendar.YEAR, -1)
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous Period",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { showCustomDateRangeDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = Triple(activeHomeDateFilter, currentMonthCalendar.timeInMillis, "$customStartDate-$customEndDate"),
                        transitionSpec = {
                            if (targetState.first != initialState.first) {
                                fadeIn() togetherWith fadeOut()
                            } else if (targetState.second > initialState.second) {
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                            } else {
                                slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "period_transition"
                    ) { target ->
                        val (targetFilter, targetTime, _) = target
                        val targetCalendar = Calendar.getInstance().apply { timeInMillis = targetTime }
                        val formattedPeriodText = when (targetFilter) {
                            "Daily" -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(targetCalendar.time)
                            "Weekly" -> {
                                val firstDayOfWeek = Calendar.MONDAY
                                val weekStart = (targetCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
                                val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
                                val sdfWeek = SimpleDateFormat("MMM d", Locale.getDefault())
                                val sdfWeekYear = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                if (weekStart.get(Calendar.YEAR) == weekEnd.get(Calendar.YEAR)) {
                                    "${sdfWeek.format(weekStart.time)} - ${sdfWeekYear.format(weekEnd.time)}"
                                } else {
                                    "${sdfWeekYear.format(weekStart.time)} - ${sdfWeekYear.format(weekEnd.time)}"
                                }
                            }
                            "Monthly" -> SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(targetCalendar.time)
                            "6 Months" -> {
                                val start6 = (targetCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -5) }
                                val sdf6 = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                "${sdf6.format(start6.time)} - ${sdf6.format(targetCalendar.time)}"
                            }
                            "Yearly" -> SimpleDateFormat("yyyy", Locale.getDefault()).format(targetCalendar.time)
                            "Custom" -> {
                                if (customStartDate != null && customEndDate != null) {
                                    val sdfCustom = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                    "${sdfCustom.format(Date(customStartDate!!))} - ${sdfCustom.format(Date(customEndDate!!))}"
                                } else {
                                    "Select Date Range"
                                }
                            }
                            else -> "All Time"
                        }
                        
                        Text(
                            text = formattedPeriodText,
                            style = if (isDiary) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                if (activeHomeDateFilter != "Custom") {
                    IconButton(
                        onClick = {
                            currentMonthCalendar = (currentMonthCalendar.clone() as Calendar).apply {
                                when (activeHomeDateFilter) {
                                    "Daily" -> add(Calendar.DAY_OF_YEAR, 1)
                                    "Weekly" -> add(Calendar.WEEK_OF_YEAR, 1)
                                    "Monthly" -> add(Calendar.MONTH, 1)
                                    "6 Months" -> add(Calendar.MONTH, 6)
                                    "Yearly" -> add(Calendar.YEAR, 1)
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next Period",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )

                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter Records",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = localFilterType == "expense",
                onClick = { 
                    localFilterType = "expense"
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Expenses")
            }
            SegmentedButton(
                selected = localFilterType == "income",
                onClick = { 
                    localFilterType = "income"
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Income")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CONTENT AREA
        if (transactions.isEmpty()) {
            com.titanbag.app.ui.components.EmptyState(
                icon = androidx.compose.material.icons.Icons.Rounded.BarChart,
                title = "No Data Registered",
                message = "Record expenses or income to view visual analytics.",
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CURRENT MONTH PIE CHART COMPONENT
                AnimatedEntranceItem(index = 0) {
                    RechartsPieChart(
                        transactions = transactions,
                        currency = currency,
                        modifier = Modifier.fillMaxWidth(),
                        type = if (localFilterType == "income") "income" else "expense"
                    )
                }

                // RECHARTS SECTOR SPENDING DASHBOARD COMPONENT (NEW)
                AnimatedEntranceItem(index = 1) {
                    RechartsSectorSpendingChart(
                        transactions = transactions,
                        currency = currency,
                        modifier = Modifier.fillMaxWidth(),
                        type = if (localFilterType == "income") "income" else "expense"
                    )
                }


                
                // 1. NET WORTH OVERVIEW
                AnimatedEntranceItem(index = 2) {
                    val investments by viewModel.allInvestments.collectAsState()
                    val totalInvestmentsVal = investments.sumOf { it.currentPrice * it.quantity }
                    val totalAccountsVal = accounts.sumOf { it.currentBalance }
                    val netWorth = totalInvestmentsVal + totalAccountsVal

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (isDiary) RoundedCornerShape(24.dp) else RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDiary) Color(0xFFFFFDE7) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, if (isDiary) Color(0xFFD4C3A3) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDiary) 2.dp else 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Total Net Worth", style = if (isDiary) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$currency${String.format(Locale.US, "%,.2f", netWorth)}",
                                style = if (isDiary) MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Investments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currency${String.format(Locale.US, "%,.2f", totalInvestmentsVal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Liquid Cash & Bank", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currency${String.format(Locale.US, "%,.2f", totalAccountsVal)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // 2. LIFE AREA DISTRIBUTION
                AnimatedEntranceItem(index = 3) {
                    val areaGroups = transactions.filter { it.type == "expense" && it.lifeAreaName != null }
                        .groupBy { it.lifeAreaName!! }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }
                        .toList()
                        .sortedByDescending { it.second }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (isDiary) RoundedCornerShape(16.dp) else RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDiary) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isDiary) Color(0xFFD4C3A3) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDiary) 2.dp else 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Spending by Life Area", style = if (isDiary) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            if (areaGroups.isEmpty()) {
                                Text("No Life Area metrics logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                val totalAreaSpend = areaGroups.sumOf { it.second }
                                areaGroups.take(5).forEach { (name, amt) ->
                                    val pct = if (totalAreaSpend > 0.0) (amt / totalAreaSpend).toFloat() else 0f
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                            Text("$currency${amt.toInt()} (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                            color = if (isDiary) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. FAMILY DISTRIBUTIONS
                AnimatedEntranceItem(index = 4) {
                    val paidByGroups = transactions.filter { it.type == "expense" }
                        .groupBy { it.paidBy ?: "Me" }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }

                    val spentForGroups = transactions.filter { it.type == "expense" }
                        .groupBy { it.spentFor ?: "Me" }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (isDiary) RoundedCornerShape(16.dp) else RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDiary) Color(0xFFF9FBE7) else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isDiary) Color(0xFFD4C3A3) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDiary) 2.dp else 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Family Spend Distribution", style = if (isDiary) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            
                            // Paid By Split
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Paid By (Who Funded)", style = if (isDiary) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                val totalPaid = paidByGroups.values.sum()
                                paidByGroups.toList().sortedByDescending { it.second }.take(3).forEach { (member, amt) ->
                                    val pct = if (totalPaid > 0.0) (amt / totalPaid).toFloat() else 0f
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(member, style = MaterialTheme.typography.bodyMedium)
                                        Text("$currency${amt.toInt()} (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }

                            // Spent For Split
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Spent For (Who Benefited)", style = if (isDiary) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                val totalSpent = spentForGroups.values.sum()
                                spentForGroups.toList().sortedByDescending { it.second }.take(3).forEach { (member, amt) ->
                                    val pct = if (totalSpent > 0.0) (amt / totalSpent).toFloat() else 0f
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(member, style = MaterialTheme.typography.bodyMedium)
                                        Text("$currency${amt.toInt()} (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. FRIEND / TAGGED SPLITS
                AnimatedEntranceItem(index = 5) {
                    val friendSpending = mutableMapOf<String, Double>()
                    transactions.filter { it.type == "expense" && !it.peopleTagged.isNullOrEmpty() }.forEach { tx ->
                        val friends = tx.peopleTagged!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        friends.forEach { friend ->
                            friendSpending[friend] = (friendSpending[friend] ?: 0.0) + tx.amount
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Friend & Tagged Spending", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            if (friendSpending.isEmpty()) {
                                Text("No friend tags recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                friendSpending.toList().sortedByDescending { it.second }.take(5).forEach { (friend, amt) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Person, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(friend, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        }
                                        Text("$currency${amt.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. VEHICLE RUNNING LOGS SUMMARY
                AnimatedEntranceItem(index = 6) {
                    val vehicles by viewModel.allVehicles.collectAsState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Vehicle Running Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            if (vehicles.isEmpty()) {
                                Text("No vehicles added to garage yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                vehicles.forEach { vehicle ->
                                    val vTx = transactions.filter { it.vehicleId == vehicle.id }
                                    val totalFuel = vTx.sumOf { it.fuelQuantity ?: 0.0 }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(vehicle.nickname, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("Odometer: ${vehicle.lastOdometer.toInt()} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                        if (totalFuel > 0.0) {
                                            Text(
                                                text = "${totalFuel.toInt()} Liters Filled",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = themeColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // BOTTOM FILTER SHEET
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.25f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filter Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(
                        onClick = {
                            viewModel.filterType.value = "All"
                            viewModel.filterCategoryIds.value = emptySet()
                            viewModel.filterAccountIds.value = emptySet()
                            viewModel.filterDateRange.value = null
                            viewModel.clearFilterTags()
                            selectedDatePeriod = "All Time"
                            customStartDate = null
                            customEndDate = null
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showFilterSheet = false
                                }
                            }
                        }
                    ) {
                        Text("Reset All")
                    }
                }

                Text("Filter by Accounts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        val selected = activeFilterAccounts.contains(account.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val current = activeFilterAccounts.toMutableSet()
                                if (selected) current.remove(account.id) else current.add(account.id)
                                viewModel.filterAccountIds.value = current
                            },
                            label = { Text(account.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = IconMapper.getIcon(account.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(android.graphics.Color.parseColor(account.color))
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Filter by Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayType = if (localFilterType == "income") "income" else "expense"
                    val filteredCats = categories.filter { it.type == displayType }
                    filteredCats.forEach { category ->
                        val selected = activeFilterCategories.contains(category.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val current = activeFilterCategories.toMutableSet()
                                if (selected) current.remove(category.id) else current.add(category.id)
                                viewModel.filterCategoryIds.value = current
                            },
                            label = { Text(category.name) },
                            leadingIcon = {
                                IconMapper.CategoryIcon(
                                    icon = category.icon,
                                    categoryName = category.name,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(android.graphics.Color.parseColor(category.color))
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showFilterSheet = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // CUSTOM DATE RANGE DIALOG
    if (showCustomDateRangeDialog) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomDateRangeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        customStartDate = dateRangePickerState.selectedStartDateMillis
                        customEndDate = dateRangePickerState.selectedEndDateMillis
                        
                        if (customStartDate != null && customEndDate != null) {
                            // Ensure time is at end of day for end date
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = customEndDate!!
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            val finalEnd = cal.timeInMillis
                            customEndDate = finalEnd
                        } else {
                            selectedDatePeriod = "All Time"
                        }
                        showCustomDateRangeDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCustomDateRangeDialog = false 
                        selectedDatePeriod = "All Time"
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(text = "Select period", modifier = Modifier.padding(16.dp))
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
