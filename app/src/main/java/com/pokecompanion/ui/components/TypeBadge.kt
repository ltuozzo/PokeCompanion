package com.pokecompanion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokecompanion.data.model.Type
import com.pokecompanion.ui.theme.TypeColors

/**
 * An official-style type badge showing the type name and an optional multiplier label.
 *
 * Example:  [  Water  ×2  ]
 */
@Composable
fun TypeBadge(type: Type, multiplier: String? = null, modifier: Modifier = Modifier) {
    val bg = TypeColors.background(type)
    val fg = TypeColors.content(type)
    val label = type.name.lowercase().replaceFirstChar { it.uppercase() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            if (multiplier != null) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = multiplier,
                    color = fg.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
