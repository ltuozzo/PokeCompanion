package com.pokecompanion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokecompanion.data.model.PokemonEntity
import com.pokecompanion.data.model.Type
import com.pokecompanion.engine.WeaknessEngine

/**
 * Full weakness breakdown card for one Pokemon.
 * Shows the Pokemon's name + own types in the header, then weakness/resistance rows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeaknessCard(pokemon: PokemonEntity, modifier: Modifier = Modifier) {
    val weakness = remember(pokemon) {
        WeaknessEngine.compute(pokemon.primaryType(), pokemon.secondaryType(), gen3Rules = false)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Header ──────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pokemon.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            TypeBadge(pokemon.primaryType())
            pokemon.secondaryType()?.let {
                Spacer(Modifier.width(4.dp))
                TypeBadge(it)
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(Modifier.height(12.dp))

        // ── Weakness rows ────────────────────────────────
        WeaknessRow(label = "×4",     types = weakness.quadWeak,     multiplier = "×4")
        WeaknessRow(label = "×2",     types = weakness.weak,         multiplier = "×2")
        WeaknessRow(label = "½",      types = weakness.halfResist,   multiplier = "½")
        WeaknessRow(label = "¼",      types = weakness.quarterResist,multiplier = "¼")
        WeaknessRow(label = "Immune", types = weakness.immune,       multiplier = "×0")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeaknessRow(label: String, types: List<Type>, multiplier: String) {
    if (types.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier
                .width(48.dp)
                .padding(top = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            types.forEach { type ->
                TypeBadge(type = type, multiplier = multiplier)
            }
        }
    }
}
