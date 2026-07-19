package com.titanbag.app.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Insight(
    val title: String,
    val description: String,
    val type: String, // "warning", "info", "success", "danger"
    val icon: String // string identifier for icon
)

object SmartInsightsEngine {

    fun generateInsights(
        transactions: List<TransactionWithDetails>,
        investments: List<Investment>,
        reminders: List<Reminder>,
        currencySymbol: String = "₹"
    ): List<Insight> {
        val insights = mutableListOf<Insight>()
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-indexed
        val currentYear = cal.get(Calendar.YEAR)

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // --- PREPARE DATA ---
        val currentMonthTxs = transactions.filter { tx ->
            try {
                val date = format.parse(tx.transactionDate.split("T").first())
                if (date != null) {
                    val c = Calendar.getInstance().apply { time = date }
                    c.get(Calendar.MONTH) + 1 == currentMonth && c.get(Calendar.YEAR) == currentYear
                } else false
            } catch (e: Exception) {
                false
            }
        }

        val previousMonthTxs = transactions.filter { tx ->
            try {
                val date = format.parse(tx.transactionDate.split("T").first())
                if (date != null) {
                    val c = Calendar.getInstance().apply { time = date }
                    val prevM = if (currentMonth == 1) 12 else currentMonth - 1
                    val prevY = if (currentMonth == 1) currentYear - 1 else currentYear
                    c.get(Calendar.MONTH) + 1 == prevM && c.get(Calendar.YEAR) == prevY
                } else false
            } catch (e: Exception) {
                false
            }
        }

        // 1. Food and Dining comparison
        val currentFood = currentMonthTxs.filter { it.categoryName.lowercase().contains("food") }.sumOf { it.amount }
        val prevFood = previousMonthTxs.filter { it.categoryName.lowercase().contains("food") }.sumOf { it.amount }
        if (prevFood > 0.0 && currentFood > prevFood) {
            val increasePercent = ((currentFood - prevFood) / prevFood * 100).toInt()
            if (increasePercent >= 10) {
                insights.add(
                    Insight(
                        title = "Food Spending Spike",
                        description = "You spent $increasePercent% more on food & dining this month compared to last month.",
                        type = "warning",
                        icon = "restaurant"
                    )
                )
            }
        }

        // 2. Vehicle Expenses & Fuel Frequency
        val currentVehicleEx = currentMonthTxs.filter { it.lifeAreaName?.lowercase() == "vehicle" || it.categoryName.lowercase().contains("maintenance") || it.categoryName.lowercase().contains("transport") }.sumOf { it.amount }
        val prevVehicleEx = previousMonthTxs.filter { it.lifeAreaName?.lowercase() == "vehicle" || it.categoryName.lowercase().contains("maintenance") || it.categoryName.lowercase().contains("transport") }.sumOf { it.amount }
        if (currentVehicleEx > prevVehicleEx) {
            val diff = (currentVehicleEx - prevVehicleEx).toInt()
            if (diff > 500) {
                insights.add(
                    Insight(
                        title = "Vehicle Expense Increase",
                        description = "Vehicle expenses increased by $currencySymbol$diff this month.",
                        type = "info",
                        icon = "directions_car"
                    )
                )
            }
        }

        val fuelCount = currentMonthTxs.filter { it.subcategoryName?.lowercase() == "fuel" }.size
        if (fuelCount >= 3) {
            insights.add(
                Insight(
                    title = "Frequent Fuel Refills",
                    description = "You purchased fuel $fuelCount times this month.",
                    type = "info",
                    icon = "local_gas_station"
                )
            )
        }

        // 3. Household spending decrease
        val currentHome = currentMonthTxs.filter { it.lifeAreaName?.lowercase() == "home" || it.lifeAreaName?.lowercase() == "household" || it.categoryName.lowercase().contains("groceries") || it.categoryName.lowercase().contains("rent") }.sumOf { it.amount }
        val prevHome = previousMonthTxs.filter { it.lifeAreaName?.lowercase() == "home" || it.lifeAreaName?.lowercase() == "household" || it.categoryName.lowercase().contains("groceries") || it.categoryName.lowercase().contains("rent") }.sumOf { it.amount }
        if (prevHome > 0.0 && currentHome < prevHome) {
            val decreasePercent = ((prevHome - currentHome) / prevHome * 100).toInt()
            if (decreasePercent >= 5) {
                insights.add(
                    Insight(
                        title = "Smart Home Spending",
                        description = "Household spending decreased by $decreasePercent% this month. Great job!",
                        type = "success",
                        icon = "home"
                    )
                )
            }
        }

        // 4. Gold Appreciation
        val goldInvestments = investments.filter { it.type.equals("Gold", ignoreCase = true) }
        if (goldInvestments.isNotEmpty()) {
            val totalCost = goldInvestments.sumOf { it.purchasePrice }
            val currentValue = goldInvestments.sumOf { it.currentPrice * it.quantity }
            if (totalCost > 0.0 && currentValue > totalCost) {
                val appreciation = ((currentValue - totalCost) / totalCost * 100).toInt()
                if (appreciation > 0) {
                    insights.add(
                        Insight(
                            title = "Gold Performance",
                            description = "Your Gold investment has appreciated by $appreciation%!",
                            type = "success",
                            icon = "trending_up"
                        )
                    )
                }
            }
        }

        // 5. School fees & Upcoming reminders (Next 7 days)
        val now = System.currentTimeMillis()
        val limit = now + 7 * 24 * 60 * 60 * 1000L // 7 days
        val activeReminders = reminders.filter { it.enabled }
        for (reminder in activeReminders) {
            try {
                val remDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(reminder.dueDate)
                if (remDate != null && remDate.time in now..limit) {
                    insights.add(
                        Insight(
                            title = "${reminder.title} Due Soon",
                            description = "Your reminder for ${reminder.title} is due on ${reminder.dueDate}.",
                            type = "danger",
                            icon = "notifications_active"
                        )
                    )
                }
            } catch (e: Exception) {}
        }

        // 6. Utility Bill Higher Than Average
        val electricityBills = transactions.filter { it.subcategoryName?.lowercase() == "electricity" }
        if (electricityBills.size >= 2) {
            val average = electricityBills.sumOf { it.amount } / electricityBills.size
            val currentBill = electricityBills.firstOrNull()?.amount ?: 0.0
            if (currentBill > average * 1.15) {
                insights.add(
                    Insight(
                        title = "High Utility Bill Alert",
                        description = "Your recent electricity bill ($currencySymbol${currentBill.toInt()}) is higher than your average ($currencySymbol${average.toInt()}).",
                        type = "danger",
                        icon = "electrical_services"
                    )
                )
            }
        }

        // 7. Spent with Friend Rahul
        // Iterate through all people tagged this year
        val taggedSpending = mutableMapOf<String, Double>()
        val currentYearStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
        
        transactions.forEach { tx ->
            try {
                val date = format.parse(tx.transactionDate.split("T").first())
                if (date != null && date.after(currentYearStart)) {
                    val friendsList = tx.peopleTagged?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    friendsList.forEach { friend ->
                        taggedSpending[friend] = (taggedSpending[friend] ?: 0.0) + tx.amount
                    }
                }
            } catch (e: Exception) {}
        }

        taggedSpending.forEach { (friend, amt) ->
            if (amt >= 2000.0) {
                insights.add(
                    Insight(
                        title = "Outing with $friend",
                        description = "You spent $currencySymbol${amt.toInt()} with $friend this year.",
                        type = "info",
                        icon = "people"
                    )
                )
            }
        }

        // Add a default positive insight if no other insights exist
        if (insights.isEmpty()) {
            insights.add(
                Insight(
                    title = "All Set!",
                    description = "Your financial operating system is running smoothly. Keep logging expenses to see insights.",
                    type = "success",
                    icon = "check_circle"
                )
            )
        }

        return insights
    }
}
