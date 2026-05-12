package com.pokecompanion.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokecompanion.data.settings.SettingsManager

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var displayId  by remember { mutableStateOf(SettingsManager.displayId) }
    var pollMs     by remember { mutableLongStateOf(SettingsManager.pollIntervalMs) }
    var defaultGen3 by remember { mutableStateOf(SettingsManager.defaultGen3Rules) }

    // Check if accessibility service is currently active.
    val isServiceEnabled = remember(Unit) {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.split(":")?.any { it.contains(context.packageName) } == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.width(8.dp))
            Text("Settings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // ── Display ID ────────────────────────────────────────────────────────
        SettingsSection(
            title = "Top screen display ID",
            caption = "Change if PokeCompanion captures the bottom screen instead of the game."
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "0  (default)", 1 to "1  (swap)").forEach { (id, label) ->
                    val selected = displayId == id
                    if (selected) {
                        Button(onClick = {}) { Text(label) }
                    } else {
                        OutlinedButton(onClick = {
                            displayId = id
                            SettingsManager.displayId = id
                        }) { Text(label) }
                    }
                }
            }
        }

        // ── Polling interval ─────────────────────────────────────────────────
        SettingsSection(
            title = "Polling interval",
            caption = "How often the top screen is checked. Lower = faster detection, higher battery use."
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500L to "0.5 s", 1_000L to "1 s", 2_000L to "2 s").forEach { (ms, label) ->
                    val selected = pollMs == ms
                    if (selected) {
                        Button(onClick = {}) { Text(label) }
                    } else {
                        OutlinedButton(onClick = {
                            pollMs = ms
                            SettingsManager.pollIntervalMs = ms
                        }) { Text(label) }
                    }
                }
            }
        }

        // ── Default ruleset ───────────────────────────────────────────────────
        SettingsSection(
            title = "Default ruleset for new profiles",
            caption = "Gen 3 rules: no Fairy type; Steel resists Ghost and Dark."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = if (defaultGen3) "Gen 3 rules" else "Gen 6+ rules  (recommended)",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = defaultGen3,
                    onCheckedChange = {
                        defaultGen3 = it
                        SettingsManager.defaultGen3Rules = it
                    }
                )
            }
        }

        // ── Accessibility service ─────────────────────────────────────────────
        SettingsSection(title = "Accessibility service") {
            if (!isServiceEnabled) {
                Text(
                    "Service not enabled — detection and calibration won't work.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                colors = if (!isServiceEnabled)
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else
                    ButtonDefaults.buttonColors()
            ) {
                Text("Open Accessibility Settings")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    caption: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            color = Color(0xFF4FC3F7),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        if (caption != null) {
            Text(
                text = caption,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}
