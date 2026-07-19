package com.titanbag.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.TitanBagViewModel
import com.titanbag.app.ui.theme.LocalVisualStyle

@Composable
fun SplashAndLockScreen(
    viewModel: TitanBagViewModel,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val biometricEnabled = settings?.biometricEnabled ?: false
    val shouldLock = isPinSet || biometricEnabled

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Auto-unlock transition check
    LaunchedEffect(isLocked, shouldLock) {
        if (!shouldLock || !isLocked) {
            onUnlockSuccess()
        }
    }

    // Auto trigger biometric on launch
    LaunchedEffect(biometricEnabled, isLocked) {
        if (biometricEnabled && isLocked) {
            onTriggerBiometric {
                onUnlockSuccess()
            }
        }
    }

    val visualStyle = LocalVisualStyle.current
    val isDiary = visualStyle == "diary"

    if (shouldLock && isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDiary) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4A0E0E).copy(alpha = 0.8f), // Leather
                                Color(0xFF2A2621)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Padlock Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(if (isDiary) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp))
                        .background(if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            if (isDiary) BorderStroke(4.dp, Color(0xFFD4C3A3)) else BorderStroke(0.dp, Color.Transparent),
                            if (isDiary) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = if (isDiary) Color(0xFF003366) else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "PiggyBag Secure",
                    style = if (isDiary) MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isPinSet) "Enter your PIN or use biometrics to unlock" else "Authenticate using system biometrics to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDiary) Color(0xFFF4ECD8).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (isPinSet) {
                    // PIN Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..6) {
                            val active = i <= enteredPin.length
                            val sizeAnim by animateDpAsState(
                                targetValue = if (active) 18.dp else 12.dp,
                                animationSpec = com.titanbag.app.ui.components.TitanBagAnimations.defaultSpring(),
                                label = "dot"
                            )
                            Box(
                                modifier = Modifier
                                    .size(sizeAnim)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) (if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.primary) 
                                        else (if (isDiary) Color(0xFFF4ECD8).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    .border(
                                        if (isDiary && !active) BorderStroke(1.dp, Color(0xFFF4ECD8)) else BorderStroke(0.dp, Color.Transparent),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Grid Numeric Keypad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(300.dp)
                    ) {
                        val keys = listOf(
                            "1", "2", "3",
                            "4", "5", "6",
                            "7", "8", "9",
                            "BIO", "0", "DEL"
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.height(300.dp)
                        ) {
                            items(keys) { key ->
                                KeypadButton(
                                    value = key,
                                    isDiary = isDiary,
                                    onClick = {
                                        errorMessage = ""
                                        when (key) {
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            "BIO" -> {
                                                if (biometricEnabled) {
                                                    onTriggerBiometric {
                                                        onUnlockSuccess()
                                                    }
                                                } else {
                                                    errorMessage = "Biometrics disabled"
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 6) {
                                                    enteredPin += key
                                                    if (enteredPin.length >= 4) {
                                                        val verified = viewModel.verifyPin(enteredPin)
                                                        if (verified) {
                                                            onUnlockSuccess()
                                                        } else if (enteredPin.length == 6) {
                                                            errorMessage = "Incorrect PIN. Please try again."
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Biometrics only mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    onTriggerBiometric {
                                        onUnlockSuccess()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = "Trigger Biometric",
                                tint = if (isDiary) Color(0xFF003366) else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Tap icon to unlock vault",
                            style = if (isDiary) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.primary
                        )

                        if (errorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    value: String,
    isDiary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .aspectRatio(1.2f)
            .clip(if (isDiary) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp))
            .background(
                if (isDiary) {
                    if (value == "DEL" || value == "BIO") Color(0xFFF4ECD8).copy(alpha = 0.2f)
                    else Color(0xFFF4ECD8)
                } else {
                    if (value == "DEL" || value == "BIO") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                if (isDiary) BorderStroke(2.dp, Color(0xFFD4C3A3)) else BorderStroke(0.dp, Color.Transparent),
                if (isDiary) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            "DEL" -> {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            "BIO" -> {
                Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = "Biometric Unlock",
                    tint = if (isDiary) Color(0xFFF4ECD8) else MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                Text(
                    text = value,
                    style = if (isDiary) MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDiary) Color(0xFF003366) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
