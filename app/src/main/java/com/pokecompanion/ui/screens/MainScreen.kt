package com.pokecompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokecompanion.data.profile.ProfileManager
import com.pokecompanion.detection.DetectionResult
import com.pokecompanion.detection.DetectionState
import com.pokecompanion.ui.components.WeaknessCard

private val BgColor = Color(0xFF1A1A2E)

/**
 * Top-level navigation host. Owns the [Screen] state and passes navigation
 * callbacks down — no navigation library needed for three screens.
 */
@Composable
fun AppContent() {
    var screen by remember { mutableIntStateOf(0) }   // 0=Main, 1=Profiles, 2=Calibration
    var calibratingProfileId by remember { mutableIntStateOf(-1) }

    when (screen) {
        0 -> MainScreen(
            onProfilesClick = { screen = 1 }
        )
        1 -> ProfileScreen(
            onBack = { screen = 0 },
            onCalibrate = { profileId ->
                calibratingProfileId = profileId
                screen = 2
            }
        )
        2 -> CalibrationScreen(
            profileId = calibratingProfileId,
            onBack = { screen = 1 }
        )
    }
}

@Composable
private fun MainScreen(onProfilesClick: () -> Unit) {
    val result        by DetectionState.result.collectAsStateWithLifecycle()
    val isAutoEnabled by DetectionState.isAutoEnabled.collectAsStateWithLifecycle()
    val activeProfile by ProfileManager.activeProfile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Profile switcher bar ─────────────────────────────────────────────
        ProfileBar(
            activeProfileName = activeProfile?.name ?: "No profile",
            onManageClick     = onProfilesClick
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // ── Content ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (val r = result) {
                is DetectionResult.None   -> EmptyState()
                is DetectionResult.Single -> WeaknessCard(r.pokemon, modifier = Modifier.fillMaxSize())
                is DetectionResult.Double -> DoubleBattleLayout(r)
            }
        }

        // ── Bottom bar ───────────────────────────────────────────────────────
        BottomBar(
            isAutoEnabled = isAutoEnabled,
            onAutoToggle  = { DetectionState.setAutoEnabled(!isAutoEnabled) },
            onSearchClick = { /* Session 6 */ }
        )
    }
}

@Composable
private fun ProfileBar(activeProfileName: String, onManageClick: () -> Unit) {
    val profiles by ProfileManager.profiles.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Scrollable row of profile chips.
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            profiles.forEach { profile ->
                val isActive = profile.name == activeProfileName
                Surface(
                    onClick = { ProfileManager.switchTo(profile) },
                    shape  = RoundedCornerShape(50),
                    color  = if (isActive) Color(0xFF4FC3F7).copy(alpha = 0.2f)
                             else Color.White.copy(alpha = 0.08f),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text  = profile.name,
                        color = if (isActive) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = onManageClick, modifier = Modifier) {
            Text("Profiles", fontSize = 12.sp)
        }
    }
}

@Composable
private fun DoubleBattleLayout(result: DetectionResult.Double) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pokemon = listOf(result.pokemon1, result.pokemon2)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = BgColor,
            contentColor     = Color.White
        ) {
            pokemon.forEachIndexed { index, p ->
                Tab(
                    selected = selectedTab == index,
                    onClick  = { selectedTab = index },
                    text     = { Text(p.name, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
        WeaknessCard(pokemon[selectedTab], modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = "No battle detected",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun BottomBar(
    isAutoEnabled: Boolean,
    onAutoToggle: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Button(
            onClick = onAutoToggle,
            colors  = ButtonDefaults.buttonColors(
                containerColor = if (isAutoEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    Color.White.copy(alpha = 0.15f)
            )
        ) {
            Text(if (isAutoEnabled) "Auto  ON" else "Auto  OFF")
        }

        OutlinedButton(onClick = onSearchClick) {
            Text("Search")
        }
    }
}
