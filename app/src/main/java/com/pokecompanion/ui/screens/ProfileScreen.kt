package com.pokecompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokecompanion.data.model.ProfileEntity
import com.pokecompanion.data.profile.ProfileManager

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onCalibrate: (profileId: Int) -> Unit
) {
    val profiles by ProfileManager.profiles.collectAsStateWithLifecycle()
    val active   by ProfileManager.activeProfile.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProfileEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(8.dp))
            Text("Profiles", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // ── Profile list ─────────────────────────────────────────────────────
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(profiles, key = { it.id }) { profile ->
                ProfileRow(
                    profile   = profile,
                    isActive  = profile.id == active?.id,
                    onSelect  = { ProfileManager.switchTo(profile) },
                    onCalibrate = { onCalibrate(profile.id) },
                    onDelete  = { deleteTarget = profile }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }

        // ── Create new ───────────────────────────────────────────────────────
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("+ New Profile")
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showCreateDialog) {
        CreateProfileDialog(
            onConfirm = { name ->
                ProfileManager.create(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Delete \"${target.name}\"?") },
            text    = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { ProfileManager.delete(target); deleteTarget = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onCalibrate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = if (isActive) Color(0xFF4FC3F7) else Color.White,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
            val cropDesc = if (profile.cropRect() != null) "Crop set" else "Full screenshot"
            Text(
                text = cropDesc,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        if (!isActive) {
            OutlinedButton(onClick = onSelect) { Text("Select") }
            Spacer(Modifier.width(6.dp))
        }

        OutlinedButton(onClick = onCalibrate) { Text("Calibrate") }
        Spacer(Modifier.width(6.dp))

        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("✕") }
    }
}

@Composable
private fun CreateProfileDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
