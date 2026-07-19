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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.Vehicle
import com.titanbag.app.data.TitanBagViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: TitanBagViewModel,
    onQuickLog: (vehicleId: Int, categoryId: Int, subcategoryId: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val vehicles by viewModel.allVehicles.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val subcategories by viewModel.allSubcategories.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }

    val themeColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Garage", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vehicleToEdit = null
                        showForm = true
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Vehicle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (vehicles.isEmpty() && !showForm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No vehicles tracked yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        vehicleToEdit = null
                        showForm = true
                    }) {
                        Text("Add Vehicle")
                    }
                }
            }
        } else if (showForm) {
            VehicleFormSheet(
                vehicle = vehicleToEdit,
                onSave = { v ->
                    if (v.id == 0) {
                        viewModel.insertVehicle(
                            nickname = v.nickname,
                            regNo = v.registrationNumber,
                            type = v.type,
                            fuelType = v.fuelType,
                            purchaseDate = v.purchaseDate,
                            insExpiry = v.insuranceExpiryDate,
                            polExpiry = v.pollutionExpiryDate,
                            roadTaxExpiry = v.roadTaxExpiryDate,
                            serviceDate = v.lastServiceDate,
                            odometer = v.lastOdometer
                        )
                    } else {
                        viewModel.updateVehicle(
                            id = v.id,
                            nickname = v.nickname,
                            regNo = v.registrationNumber,
                            type = v.type,
                            fuelType = v.fuelType,
                            purchaseDate = v.purchaseDate,
                            insExpiry = v.insuranceExpiryDate,
                            polExpiry = v.pollutionExpiryDate,
                            roadTaxExpiry = v.roadTaxExpiryDate,
                            serviceDate = v.lastServiceDate,
                            odometer = v.lastOdometer
                        )
                    }
                    showForm = false
                },
                onDelete = { v ->
                    viewModel.deleteVehicle(v)
                    showForm = false
                },
                onDismiss = { showForm = false }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(vehicles) { vehicle ->
                    // Gather fuel & maintenance logs
                    val vTxList = transactions.filter { it.vehicleId == vehicle.id }
                    val fuelLogs = vTxList.filter { it.subcategoryName?.lowercase()?.contains("fuel") == true }
                    
                    val insDaysLeft = daysBetweenTodayAnd(vehicle.insuranceExpiryDate)
                    val polDaysLeft = daysBetweenTodayAnd(vehicle.pollutionExpiryDate)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vehicle.nickname,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${vehicle.type.uppercase()} • ${vehicle.registrationNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = {
                                        vehicleToEdit = vehicle
                                        showForm = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = themeColor)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Odometer Reading
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Odometer", style = MaterialTheme.typography.labelSmall, color = themeColor)
                                        Text("${vehicle.lastOdometer.toInt()} km", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                // Fuel Fill-ups
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Fuel Fillups", style = MaterialTheme.typography.labelSmall, color = themeColor)
                                        Text("${fuelLogs.size} Times", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            // Renewal Alerts
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Expiries & Renewals", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Insurance
                            ExpiryRow(label = "Insurance", date = vehicle.insuranceExpiryDate, daysLeft = insDaysLeft)
                            // Pollution
                            ExpiryRow(label = "Pollution (PUC)", date = vehicle.pollutionExpiryDate, daysLeft = polDaysLeft)
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick Log Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Find categories & subcategories to prefill
                                val transportCat = categories.find { it.name.lowercase().contains("car") || it.name.lowercase().contains("transport") }
                                val fuelSub = subcategories.find { it.name.lowercase() == "fuel" }
                                val maintSub = subcategories.find { it.name.lowercase().contains("maintenance") || it.name.lowercase().contains("repair") }

                                OutlinedButton(
                                    onClick = {
                                        if (transportCat != null) {
                                            onQuickLog(vehicle.id, transportCat.id, fuelSub?.id)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log Fuel", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (transportCat != null) {
                                            onQuickLog(vehicle.id, transportCat.id, maintSub?.id)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Service Log", fontSize = 12.sp)
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
fun ExpiryRow(label: String, date: String?, daysLeft: Long?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (daysLeft != null && daysLeft <= 30) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (daysLeft != null && daysLeft <= 30) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }

        if (date.isNullOrEmpty()) {
            Text("Not set", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        } else {
            val alertStr = if (daysLeft != null && daysLeft <= 0) "Expired" else if (daysLeft != null) "$daysLeft days left" else ""
            Text(
                text = "$date ($alertStr)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (daysLeft != null && daysLeft <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormSheet(
    vehicle: Vehicle?,
    onSave: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nickname by remember { mutableStateOf(vehicle?.nickname ?: "") }
    var regNo by remember { mutableStateOf(vehicle?.registrationNumber ?: "") }
    var type by remember { mutableStateOf(vehicle?.type ?: "Car") }
    var fuelType by remember { mutableStateOf(vehicle?.fuelType ?: "Petrol") }
    var purchaseDate by remember { mutableStateOf(vehicle?.purchaseDate ?: "") }
    var insExpiry by remember { mutableStateOf(vehicle?.insuranceExpiryDate ?: "") }
    var polExpiry by remember { mutableStateOf(vehicle?.pollutionExpiryDate ?: "") }
    var roadTaxExpiry by remember { mutableStateOf(vehicle?.roadTaxExpiryDate ?: "") }
    var serviceDate by remember { mutableStateOf(vehicle?.lastServiceDate ?: "") }
    var odometerStr by remember { mutableStateOf(vehicle?.lastOdometer?.toInt()?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (vehicle == null) "Add Vehicle" else "Edit Vehicle",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Vehicle Nickname (e.g. My Honda)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = regNo,
            onValueChange = { regNo = it },
            label = { Text("Registration Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Type dropdown list
        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text("Vehicle Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                listOf("Car", "Bike", "Scooter", "Electric Vehicle", "Other").forEach { item ->
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

        // Fuel Type dropdown
        var fuelExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = fuelExpanded,
            onExpandedChange = { fuelExpanded = !fuelExpanded }
        ) {
            OutlinedTextField(
                value = fuelType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fuel Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = fuelExpanded,
                onDismissRequest = { fuelExpanded = false }
            ) {
                listOf("Petrol", "Diesel", "CNG", "Electric", "Hybrid").forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            fuelType = item
                            fuelExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = odometerStr,
            onValueChange = { odometerStr = it },
            label = { Text("Opening Odometer (km)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        // Date selection helpers
        DatePickerField(label = "Purchase Date", date = purchaseDate, onDateSelected = { purchaseDate = it })
        DatePickerField(label = "Insurance Expiry Date", date = insExpiry, onDateSelected = { insExpiry = it })
        DatePickerField(label = "Pollution Certificate Expiry", date = polExpiry, onDateSelected = { polExpiry = it })
        DatePickerField(label = "Road Tax Expiry Date", date = roadTaxExpiry, onDateSelected = { roadTaxExpiry = it })
        DatePickerField(label = "Last Service Date", date = serviceDate, onDateSelected = { serviceDate = it })

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
                    if (nickname.isNotBlank() && regNo.isNotBlank()) {
                        val finalOdometer = odometerStr.toDoubleOrNull() ?: 0.0
                        onSave(
                            Vehicle(
                                id = vehicle?.id ?: 0,
                                nickname = nickname,
                                registrationNumber = regNo,
                                type = type,
                                fuelType = fuelType,
                                purchaseDate = purchaseDate.ifEmpty { "2026-01-01" },
                                insuranceExpiryDate = insExpiry.takeIf { it.isNotEmpty() },
                                pollutionExpiryDate = polExpiry.takeIf { it.isNotEmpty() },
                                roadTaxExpiryDate = roadTaxExpiry.takeIf { it.isNotEmpty() },
                                lastServiceDate = serviceDate.takeIf { it.isNotEmpty() },
                                lastOdometer = finalOdometer,
                                userId = vehicle?.userId ?: ""
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

        if (vehicle != null) {
            TextButton(
                onClick = { onDelete(vehicle) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Vehicle")
            }
        }
    }
}

@Composable
fun DatePickerField(label: String, date: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedTextField(
        value = date,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> onDateSelected(String.format("%04d-%02d-%02d", y, m + 1, d)) },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

fun daysBetweenTodayAnd(targetDate: String?): Long? {
    if (targetDate.isNullOrEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val target = sdf.parse(targetDate)
        val today = sdf.parse(sdf.format(Date()))
        if (target != null && today != null) {
            val diffMs = target.time - today.time
            diffMs / (24 * 60 * 60 * 1000)
        } else null
    } catch (e: Exception) {
        null
    }
}
