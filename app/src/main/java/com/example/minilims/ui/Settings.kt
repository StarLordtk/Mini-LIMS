package com.example.minilims.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } }
    )
}

@Composable
fun SettingsScreen(vm: LimsViewModel) {
    val ctx = LocalContext.current
    val lab by vm.labName.collectAsState()
    var labDialog by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var confirmUndo by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val hasUndo = remember { Files.hasUndo(ctx) }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) vm.saveBackupTo(uri) { ok ->
            message = if (ok) "Backup saved." else "Couldn't save the backup."
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }

    fun finish(err: String?) {
        busy = false
        if (err == null) Files.restartApp(ctx) else message = err
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        SectionLabel("Laboratory")
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
            SettingRow(Icons.Default.Edit, "Lab name", lab) { labDialog = true }
        }

        SectionLabel("Backup and restore")
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
            Column {
                SettingRow(Icons.Default.CheckCircle, "Save backup to device", "Choose where to store a copy of all your data") {
                    createBackup.launch(Files.backupName())
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Default.Share, "Share backup", "Send a copy by email, Drive, WhatsApp…") { vm.shareBackup() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(Icons.Default.Refresh, "Restore from backup", "Replace current data with a saved backup file") {
                    pickBackup.launch(arrayOf("*/*"))
                }
                if (hasUndo) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(Icons.Default.Refresh, "Undo last restore", "Go back to the data you had before restoring") {
                        confirmUndo = true
                    }
                }
            }
        }

        SectionLabel("About")
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
            SettingRow(Icons.Default.Info, "MiniLIMS 0.2", "Works offline. All data stays on this device.") {}
        }
        Spacer(Modifier.height(24.dp))
    }

    if (labDialog) FormDialog("Lab name (shown on reports)", listOf("Name"), listOf(lab),
        onDismiss = { labDialog = false }) { v -> vm.setLabName(v[0].trim()); labDialog = false }

    restoreUri?.let { uri ->
        ConfirmDialog(
            "Restoring replaces ALL current patients, tests and samples with the contents of this backup. " +
                "A safety copy of your current data is kept so you can undo. The app will restart.",
            "Restore",
            onConfirm = { restoreUri = null; busy = true; vm.restoreFrom(uri) { finish(it) } },
            onDismiss = { restoreUri = null }
        )
    }
    if (confirmUndo) ConfirmDialog(
        "Go back to the data you had before the last restore? The app will restart.", "Undo restore",
        onConfirm = { confirmUndo = false; busy = true; vm.undoRestore { finish(it) } },
        onDismiss = { confirmUndo = false }
    )
    message?.let { MessageDialog(it) { message = null } }
}
