package com.pokecompanion.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokecompanion.data.profile.ProfileManager
import com.pokecompanion.detection.DetectionState
import kotlin.math.abs
import kotlin.math.min

/**
 * Screen that shows a screenshot of the top screen and lets the user drag a
 * rectangle over the enemy name area.  The resulting crop is saved to the profile.
 *
 * Flow:
 *  1. User taps "Capture" → service takes the next screenshot and posts it here.
 *  2. User drags to draw a rectangle over the enemy name region.
 *  3. User taps "Save" → crop is persisted to the profile.
 */
@Composable
fun CalibrationScreen(profileId: Int, onBack: () -> Unit) {
    val bitmap by DetectionState.calibrationBitmap.collectAsStateWithLifecycle()
    val pending by DetectionState.pendingCalibration.collectAsStateWithLifecycle()

    // Drag state in screen (display) coordinates.
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd   by remember { mutableStateOf<Offset?>(null) }
    // Actual layout size of the canvas/image area.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

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
            Text(
                "Calibrate crop region",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        // ── Image + drag overlay ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { canvasSize = it }
                .pointerInput(bitmap) {
                    if (bitmap == null) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStart = offset
                            dragEnd = offset
                        },
                        onDrag = { change, _ ->
                            dragEnd = change.position
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw the screenshot scaled to fit.
                    val scale = min(
                        size.width / bmp.width,
                        size.height / bmp.height
                    )
                    val imgW = bmp.width * scale
                    val imgH = bmp.height * scale
                    val imgLeft = (size.width - imgW) / 2f
                    val imgTop  = (size.height - imgH) / 2f

                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(imgLeft.toInt(), imgTop.toInt()),
                        dstSize   = androidx.compose.ui.unit.IntSize(imgW.toInt(), imgH.toInt())
                    )

                    // Dim area outside crop rect.
                    val s = dragStart
                    val e = dragEnd
                    if (s != null && e != null) {
                        val rectLeft   = min(s.x, e.x)
                        val rectTop    = min(s.y, e.y)
                        val rectWidth  = abs(e.x - s.x)
                        val rectHeight = abs(e.y - s.y)

                        drawRect(
                            color = Color.Black.copy(alpha = 0.45f),
                            topLeft = Offset(imgLeft, imgTop),
                            size = Size(imgW, imgH)
                        )
                        // Clear the selected region so it looks "unmasked".
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(rectLeft, rectTop),
                            size = Size(rectWidth, rectHeight),
                            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                        )
                        // Selection border.
                        drawRect(
                            color = Color(0xFF4FC3F7),
                            topLeft = Offset(rectLeft, rectTop),
                            size = Size(rectWidth, rectHeight),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            } else if (pending) {
                Text("Waiting for screenshot…", color = Color.White.copy(alpha = 0.5f))
            } else {
                Text(
                    "Tap Capture, then drag a box\nover the enemy name area.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // ── Bottom bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    dragStart = null
                    dragEnd = null
                    DetectionState.clearCalibrationBitmap()
                    DetectionState.pendingCalibration.value = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Capture")
            }

            Spacer(Modifier.width(8.dp))

            val canSave = bitmap != null && dragStart != null && dragEnd != null
            Button(
                onClick = {
                    val bmp = bitmap ?: return@Button
                    val s = dragStart ?: return@Button
                    val e = dragEnd ?: return@Button

                    // Map display coords → bitmap coords.
                    val scale = min(
                        canvasSize.width.toFloat() / bmp.width,
                        canvasSize.height.toFloat() / bmp.height
                    )
                    val imgW    = bmp.width * scale
                    val imgH    = bmp.height * scale
                    val imgLeft = (canvasSize.width - imgW) / 2f
                    val imgTop  = (canvasSize.height - imgH) / 2f

                    fun toX(px: Float) = ((px - imgLeft) / scale).toInt().coerceIn(0, bmp.width)
                    fun toY(py: Float) = ((py - imgTop)  / scale).toInt().coerceIn(0, bmp.height)

                    ProfileManager.saveCrop(
                        profileId = profileId,
                        left   = minOf(toX(s.x), toX(e.x)),
                        top    = minOf(toY(s.y), toY(e.y)),
                        right  = maxOf(toX(s.x), toX(e.x)),
                        bottom = maxOf(toY(s.y), toY(e.y))
                    )
                    DetectionState.clearCalibrationBitmap()
                    onBack()
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
