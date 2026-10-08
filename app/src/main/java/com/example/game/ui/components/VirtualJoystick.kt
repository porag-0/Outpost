package com.example.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    deadZone: Float = 0.06f,
    onMove: (x: Float, y: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val thumbOffsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val currentOnMove by rememberUpdatedState(onMove)

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(size, deadZone) {
                val radiusPx = this.size.width / 2f
                val maxDist = radiusPx * 0.72f

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    val center = Offset(radiusPx, radiusPx)
                    val initialDrag = down.position - center
                    val initialDist = hypot(initialDrag.x, initialDrag.y)
                    val clampedDist = initialDist.coerceAtMost(maxDist)
                    val norm = if (initialDist > 0f) initialDrag / initialDist else Offset.Zero
                    val targetOffset = norm * clampedDist

                    coroutineScope.launch {
                        thumbOffsetAnim.snapTo(targetOffset)
                    }

                    // Normalized vector calculation with dead-zone and non-linear response
                    fun dispatchMove(offset: Offset) {
                        val d = hypot(offset.x, offset.y)
                        val rawNormalized = (d / maxDist).coerceIn(0f, 1f)
                        if (rawNormalized <= deadZone) {
                            currentOnMove(0f, 0f)
                        } else {
                            // Remap range [deadZone, 1.0] to [0.0, 1.0] with quadratic sensitivity for fine micro-adjustments
                            val activeMagnitude = (rawNormalized - deadZone) / (1f - deadZone)
                            val curvedMagnitude = activeMagnitude * activeMagnitude * 0.35f + activeMagnitude * 0.65f
                            val dirX = if (d > 0f) offset.x / d else 0f
                            val dirY = if (d > 0f) offset.y / d else 0f
                            currentOnMove(dirX * curvedMagnitude, dirY * curvedMagnitude)
                        }
                    }

                    dispatchMove(targetOffset)

                    var currentOffset = targetOffset

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val dragDelta = change.positionChange()
                            if (dragDelta != Offset.Zero) {
                                change.consume()
                                val nextOffset = currentOffset + dragDelta
                                val dist = hypot(nextOffset.x, nextOffset.y)
                                currentOffset = if (dist > maxDist) (nextOffset / dist) * maxDist else nextOffset

                                coroutineScope.launch {
                                    thumbOffsetAnim.snapTo(currentOffset)
                                }
                                dispatchMove(currentOffset)
                            }
                        } else {
                            // Touch released: spring snap back to neutral center smoothly and zero velocity
                            change.consume()
                            coroutineScope.launch {
                                thumbOffsetAnim.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 850f)
                                )
                            }
                            currentOnMove(0f, 0f)
                            break
                        }
                    }
                }
            }
    ) {
        val thumbOffset = thumbOffsetAnim.value

        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width * 0.44f
            val thumbRadius = this.size.width * 0.20f

            // Outer Base Ring with Sci-Fi glowing background gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4400E5FF), Color(0x18001020), Color(0x00000000)),
                    center = center,
                    radius = outerRadius * 1.08f
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0x8800E5FF),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner Boundary Track Ring
            drawCircle(
                color = Color(0x3300E5FF),
                radius = outerRadius * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // Crosshair ticks
            val tickLen = 9.dp.toPx()
            drawLine(Color(0x7700E5FF), center + Offset(-outerRadius, 0f), center + Offset(-outerRadius + tickLen, 0f), strokeWidth = 2.2f)
            drawLine(Color(0x7700E5FF), center + Offset(outerRadius - tickLen, 0f), center + Offset(outerRadius, 0f), strokeWidth = 2.2f)
            drawLine(Color(0x7700E5FF), center + Offset(0f, -outerRadius), center + Offset(0f, -outerRadius + tickLen), strokeWidth = 2.2f)
            drawLine(Color(0x7700E5FF), center + Offset(0f, outerRadius - tickLen), center + Offset(0f, outerRadius), strokeWidth = 2.2f)

            // Dynamic deflection tether indicator line from base center to thumb knob
            val thumbCenter = center + thumbOffset
            val currentOffsetMag = hypot(thumbOffset.x, thumbOffset.y)
            if (currentOffsetMag > 4f) {
                drawLine(
                    color = Color(0x5500E5FF),
                    start = center,
                    end = thumbCenter,
                    strokeWidth = 2.0f,
                    cap = StrokeCap.Round
                )
            }

            // Inner Thumb Stick with high-tech concentric rings and center cap
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00B0FF), Color(0xFF01579B)),
                    center = thumbCenter,
                    radius = thumbRadius
                ),
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
                color = Color(0x66E0F7FA),
                radius = thumbRadius * 0.60f,
                center = thumbCenter,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = thumbRadius * 0.28f,
                center = thumbCenter
            )
        }
    }
}
