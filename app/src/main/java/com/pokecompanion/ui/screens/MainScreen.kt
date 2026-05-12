package com.pokecompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokecompanion.data.model.PokemonEntity
import com.pokecompanion.data.profile.ProfileManager
import com.pokecompanion.detection.DetectionResult
import com.pokecompanion.detection.DetectionState
import com.pokecompanion.ui.components.TypeBadge
import com.pokecompanion.ui.components.WeaknessCard
import kotlinx.coroutines.delay

private val BgColor = Color(0xFF1A1A2E)

/**
 * Top-level navigation host.  Owns the [Screen] state and passes navigation
 * callbacks down — no navigation library needed for four screens.
 */
@Composable
fun AppContent() {
    var screen by remember { mutableIntStateOf(0) }  // 0=Main, 1=Profiles, 2=Calibration, 3=Settings
    var calibratingProfileId by remember { mutableIntStateOf(-1) }

    when (screen) {
        0 -> MainScreen(
            onProfilesClick  = { screen = 1 },
            onSettingsClick  = { screen = 3 }
        )
        1 -> ProfileScreen(
            onBack       = { screen = 0 },
            onCalibrate  = { profileId -> calibratingProfileId = profileId; screen = 2 }
        )
        2 -> CalibrationScreen(profileId = calibratingProfileId, onBack = { screen = 1 })
        3 -> SettingsScreen(onBack = { screen = 0 })
    }
}

@Composable
private fun MainScreen(onProfilesClick: () -> Unit, onSettingsClick: () -> Unit) {
    val result        by DetectionState.result.collectAsStateWithLifecycle()
    val isAutoEnabled by DetectionState.isAutoEnabled.collectAsStateWithLifecycle()
    val activeProfile by ProfileManager.activeProfile.collectAsStateWithLifecycle()

    var isSearchMode    by remember { mutableStateOf(false) }
    var manualPokemon   by remember { mutableStateOf<PokemonEntity?>(null) }

    // Check accessibility service status (read-only, computed once per composition).
    val context = LocalContext.current
    val isServiceEnabled = remember {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.split(":")?.any { it.contains(context.packageName) } == true
    }

    val gen3Rules = activeProfile?.gen3Rules ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // ── Service-not-enabled banner ────────────────────────────────────────
        if (!isServiceEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Accessibility service not enabled",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onSettingsClick) { Text("Fix", fontSize = 12.sp) }
            }
        }

        // ── Profile bar ───────────────────────────────────────────────────────
        ProfileBar(
            activeProfileName = activeProfile?.name ?: "No profile",
            onProfilesClick   = onProfilesClick,
            onSettingsClick   = onSettingsClick
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // ── Content ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                isSearchMode -> SearchView(
                    gen3Rules  = gen3Rules,
                    onSelected = { pokemon ->
                        manualPokemon = pokemon
                        isSearchMode  = false
                    },
                    onDismiss  = { isSearchMode = false }
                )

                manualPokemon != null ->
                    WeaknessCard(manualPokemon!!, gen3Rules = gen3Rules, modifier = Modifier.fillMaxSize())

                else -> when (val r = result) {
                    is DetectionResult.None   -> EmptyState()
                    is DetectionResult.Single -> WeaknessCard(r.pokemon, gen3Rules = gen3Rules, modifier = Modifier.fillMaxSize())
                    is DetectionResult.Double -> DoubleBattleLayout(r, gen3Rules = gen3Rules)
                }
            }
        }

        // ── Bottom bar ────────────────────────────────────────────────────────
        BottomBar(
            isAutoEnabled = isAutoEnabled,
            isSearchMode  = isSearchMode,
            onAutoToggle  = {
                val newAuto = !isAutoEnabled
                DetectionState.setAutoEnabled(newAuto)
                if (newAuto) {
                    // Resuming auto — clear manual selection so battle detection takes over.
                    manualPokemon = null
                    isSearchMode  = false
                }
            },
            onSearchClick = {
                isSearchMode = !isSearchMode
                if (isSearchMode) DetectionState.setAutoEnabled(false)
            }
        )
    }
}

// ── Profile bar ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileBar(
    activeProfileName: String,
    onProfilesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val profiles by ProfileManager.profiles.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                             else Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text       = profile.name,
                        color      = if (isActive) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 12.sp,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))
        OutlinedButton(onClick = onProfilesClick) { Text("Profiles", fontSize = 12.sp) }
        Spacer(Modifier.width(4.dp))
        OutlinedButton(onClick = onSettingsClick) { Text("⚙", fontSize = 14.sp) }
    }
}

// ── 2v2 layout ────────────────────────────────────────────────────────────────

@Composable
private fun DoubleBattleLayout(result: DetectionResult.Double, gen3Rules: Boolean) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pokemon = listOf(result.pokemon1, result.pokemon2)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = BgColor, contentColor = Color.White) {
            pokemon.forEachIndexed { index, p ->
                Tab(
                    selected = selectedTab == index,
                    onClick  = { selectedTab = index },
                    text     = { Text(p.name, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
        WeaknessCard(pokemon[selectedTab], gen3Rules = gen3Rules, modifier = Modifier.fillMaxSize())
    }
}

// ── Search ────────────────────────────────────────────────────────────────────

@Composable
private fun SearchView(
    gen3Rules: Boolean,
    onSelected: (PokemonEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var query   by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PokemonEntity>>(emptyList()) }
    var selectedPokemon by remember { mutableStateOf<PokemonEntity?>(null) }

    // Debounced DB query — fires 250 ms after the last keystroke.
    LaunchedEffect(query) {
        delay(250)
        results = if (query.length >= 2) ProfileManager.search(query) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Search field
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it; selectedPokemon = null },
            placeholder   = { Text("Search Pokemon…") },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        if (selectedPokemon != null) {
            // Show weakness card for the chosen Pokemon.
            WeaknessCard(selectedPokemon!!, gen3Rules = gen3Rules, modifier = Modifier.fillMaxSize())
        } else {
            // Results list.
            if (results.isEmpty() && query.length >= 2) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results for \"$query\"", color = Color.White.copy(alpha = 0.35f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.id }) { pokemon ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPokemon = pokemon; onSelected(pokemon) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pokemon.name, color = Color.White, modifier = Modifier.weight(1f))
                            TypeBadge(pokemon.primaryType())
                            pokemon.secondaryType()?.let {
                                Spacer(Modifier.width(4.dp))
                                TypeBadge(it)
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text     = "No battle detected",
            color    = Color.White.copy(alpha = 0.3f),
            fontSize = 16.sp
        )
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────

@Composable
private fun BottomBar(
    isAutoEnabled: Boolean,
    isSearchMode: Boolean,
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

        OutlinedButton(
            onClick = onSearchClick,
            colors  = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isSearchMode) Color(0xFF4FC3F7) else Color.White
            )
        ) {
            Text(if (isSearchMode) "Close" else "Search")
        }
    }
}
