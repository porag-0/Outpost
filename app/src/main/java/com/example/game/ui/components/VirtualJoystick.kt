package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onMove: (x: Float, y: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val radiusPx = this.size.width / 2f
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radiusPx, radiusPx)
                        val drag = offset - center
                        val dist = hypot(drag.x, drag.y)
                        val clampedDist = dist.coerceAtMost(radiusPx * 0.75f)
                        val norm = if (dist > 0f) drag / dist else Offset.Zero
                        thumbOffset = norm * clampedDist
                        onMove(thumbOffset.x / (radiusPx * 0.75f), thumbOffset.y / (radiusPx * 0.75f))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = hypot(newOffset.x, newOffset.y)
                        val maxDist = radiusPx * 0.75f
                        thumbOffset = if (dist > maxDist) (newOffset / dist) * maxDist else newOffset
                        onMove(thumbOffset.x / maxDist, thumbOffset.y / maxDist)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width * 0.44f
            val thumbRadius = this.size.width * 0.20f

            // Outer Base Ring
            drawCircle(
                color = Color(0x3300E5FF),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0x8800E5FF),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Crosshair ticks
            val tickLen = 8.dp.toPx()
            drawLine(Color(0x6600E5FF), center + Offset(-outerRadius, 0f), center + Offset(-outerRadius + tickLen, 0f), strokeWidth = 2f)
            drawLine(Color(0x6600E5FF), center + Offset(outerRadius - tickLen, 0f), center + Offset(outerRadius, 0f), strokeWidth = 2f)
            drawLine(Color(0x6600E5FF), center + Offset(0f, -outerRadius), center + Offset(0f, -outerRadius + tickLen), strokeWidth = 2f)
            drawLine(Color(0x6600E5FF), center + Offset(0f, outerRadius - tickLen), center + Offset(0f, outerRadius), strokeWidth = 2f)

            // Inner Thumb Stick
            val thumbCenter = center + thumbOffset
            drawCircle(
                color = Color(0xAA0277BD),
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = thumbRadius,
                center = thumbCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFE0F7FA),
                radius = thumbRadius * 0.35f,
                center = thumbCenter
            )
        }
    }
}
