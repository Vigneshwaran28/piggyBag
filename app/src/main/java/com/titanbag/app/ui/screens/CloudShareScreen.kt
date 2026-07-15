package com.titanbag.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanbag.app.data.CloudAuthState
import com.titanbag.app.data.CloudJournalViewModel
import com.titanbag.app.ui.components.TitanBagMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudShareScreen(
    viewModel: CloudJournalViewModel,
    onBack: () -> Unit,
    onNavigateToPartner: () -> Unit,
    onNavigateToSharedJournals: () -> Unit,
    onNavigateToSyncStatus: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val partner by viewModel.partnerRelation.collectAsState(initial = null)
    val syncQueueSize by viewModel.pendingQueueCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Share", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Partner Section
            SectionHeader("Partner Sharing", Icons.Rounded.People)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    if (partner != null) {
                        TitanBagMenuItem(
                            icon = Icons.Rounded.Person,
                            title = "Connected Partner",
                            subtitle = "Linked with your partner",
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToPartner
                        )
                    } else {
                        TitanBagMenuItem(
                            icon = Icons.Rounded.PersonAdd,
                            title = "Add Partner",
                            subtitle = "Connect with a partner using code",
                            onClick = onNavigateToPartner
                        )
                    }
                }
            }

            // 2. Shared Journals Section
            SectionHeader("Shared Journals", Icons.Rounded.AutoStories)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    TitanBagMenuItem(
                        icon = Icons.Rounded.LibraryAdd,
                        title = "Manage Journals",
                        subtitle = "Create or join group expense journals",
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToSharedJournals
                    )
                }
            }

            // 3. Sync Status Section
            SectionHeader("Synchronization", Icons.Rounded.Sync)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    TitanBagMenuItem(
                        icon = Icons.Rounded.CloudSync,
                        title = "Sync Status",
                        subtitle = if (syncQueueSize > 0) "$syncQueueSize items pending" else "Everything up to date",
                        iconTint = if (syncQueueSize > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        onClick = onNavigateToSyncStatus
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Option
            if (authState is CloudAuthState.LoggedIn) {
                Button(
                    onClick = { viewModel.logout(); onBack() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from Cloud")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
