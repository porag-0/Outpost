package com.example.game.ui.components

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.*

enum class CelestialPlanet(
    val title: String,
    val subtitle: String,
    val gravity: String,
    val temperature: String,
    val atmosphere: String,
    val distanceAu: String,
    val outpostName: String,
    val primaryColor: Color,
    val accentColor: Color,
    val isMarsEngine: Boolean
) {
    MOON(
        title = "MOON (LUNA)",
        subtitle = "Earth's Natural Satellite • Oceanus Procellarum",
        gravity = "1.62 m/s² (0.166 g)",
        temperature = "-130°C to +120°C",
        atmosphere = "Exosphere / Vacuum (10⁻¹⁰ Pa)",
        distanceAu = "0.00257 AU (384,400 km)",
        outpostName = "Shackleton Lunar Outpost",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFFE0F7FA),
        isMarsEngine = false
    ),
    MARS(
        title = "MARS (ARES)",
        subtitle = "The Red Planet • Valles Marineris Sector",
        gravity = "3.72 m/s² (0.379 g)",
        temperature = "-63°C avg (-140°C to +20°C)",
        atmosphere = "Thin CO₂ (0.636 kPa, 95% CO₂)",
        distanceAu = "1.524 AU (227.9M km)",
        outpostName = "Ares Outpost Alpha",
        primaryColor = Color(0xFFFF7043),
        accentColor = Color(0xFFFFCCBC),
        isMarsEngine = true
    ),
    EARTH(
        title = "EARTH (TERRA)",
        subtitle = "Homeworld • Low Earth Orbit Spacedock",
        gravity = "9.81 m/s² (1.000 g)",
        temperature = "+15°C avg (-89°C to +58°C)",
        atmosphere = "101.3 kPa (78% N₂, 21% O₂)",
        distanceAu = "1.000 AU (149.6M km)",
        outpostName = "Gateway Orbital Spacedock",
        primaryColor = Color(0xFF40C4FF),
        accentColor = Color(0xFFB2EBF2),
        isMarsEngine = false
    ),
    JUPITER(
        title = "JUPITER & EUROPA",
        subtitle = "Gas Giant System • Jovian Subsurface Ocean",
        gravity = "24.79 m/s² (Europa: 1.31 m/s²)",
        temperature = "-110°C avg (Europa: -170°C)",
        atmosphere = "Supercritical H₂/He / H₂O ice",
        distanceAu = "5.204 AU (778.5M km)",
        outpostName = "Europa Cryo-Drill Array",
        primaryColor = Color(0xFFFFB300),
        accentColor = Color(0xFFFFE082),
        isMarsEngine = false
    ),
    SATURN(
        title = "SATURN & TITAN",
        subtitle = "Ringed Sovereign • Hydrocarbon Atmosphere",
        gravity = "10.44 m/s² (Titan: 1.35 m/s²)",
        temperature = "-140°C avg (Titan: -179°C)",
        atmosphere = "H₂/He rings (Titan: 146.7 kPa N₂/CH₄)",
        distanceAu = "9.582 AU (1.43B km)",
        outpostName = "Titan Hydrocarbon Refinery",
        primaryColor = Color(0xFFFFD54F),
        accentColor = Color(0xFFFFF9C4),
        isMarsEngine = true
    )
}

@Composable
fun SpaceCentricBackground(
    selectedPlanet: CelestialPlanet,
    modifier: Modifier = Modifier,
    showMeteors: Boolean = true,
    showOrbitalRings: Boolean = true,
    planetScale: Float = 1.0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "space_motion")

    // Continuous star twinkle and celestial rotation clock
    val celestialRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "celestial_rot"
    )

    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    val nebulaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebula_pulse"
    )

    val meteorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "meteor"
    )

    // Precomputed deterministic stars
    val stars = remember {
        val list = ArrayList<StarData>(160)
        for (i in 0 until 160) {
            val rx = sin(i * 137.5f) * 0.5f + 0.5f
            val ry = cos(i * 93.3f) * 0.5f + 0.5f
            val baseRadius = when {
                i % 24 == 0 -> 2.8f // Bright alpha star with diffraction spikes
                i % 8 == 0 -> 1.8f  // Mid-magnitude star
                i % 3 == 0 -> 1.2f  // Faint star
                else -> 0.8f        // Deep background dust star
            }
            // Spectral types: blue-white, solar yellow, ruby red, pure white
            val spectralColor = when (i % 5) {
                0 -> Color(0xFFB3E5FC) // Type O/B Blue-White
                1 -> Color(0xFFFFF9C4) // Type G Solar Gold
                2 -> Color(0xFFFFCCBC) // Type M Red Giant
                3 -> Color(0xFFE0F7FA) // Type A Cyan
                else -> Color.White
            }
            list.add(StarData(rx, ry, baseRadius, spectralColor, phase = (i * 0.47f)))
        }
        list
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val w = size.width
            val h = size.height

            if (w <= 0f || h <= 0f) return@Canvas

            // 1. Deep Space Cosmic Void with Dynamic Multi-Spectral Nebulae
            val bgGradient = RadialGradient(
                w * 0.5f,
                h * 0.45f,
                max(w, h) * 0.85f,
                intArrayOf(
                    when (selectedPlanet) {
                        CelestialPlanet.MARS -> 0xFF2A0904.toInt()
                        CelestialPlanet.MOON -> 0xFF081526.toInt()
                        CelestialPlanet.EARTH -> 0xFF04182B.toInt()
                        CelestialPlanet.JUPITER -> 0xFF261805.toInt()
                        CelestialPlanet.SATURN -> 0xFF231C08.toInt()
                    },
                    0xFF060D17.toInt(),
                    0xFF010408.toInt()
                ),
                floatArrayOf(0.0f, 0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val bgPaint = Paint().apply {
                isAntiAlias = true
                shader = bgGradient
            }
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // Deep Space Ionized Nebula Swirls
            val nebPaint1 = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    w * 0.25f,
                    h * 0.30f,
                    w * 0.55f,
                    intArrayOf(
                        (0x356A1B9A.toLong() and 0x00FFFFFF or ((nebulaPulse * 0x30).toInt().toLong() shl 24)).toInt(),
                        0x1200E5FF.toInt(),
                        0x00000000
                    ),
                    floatArrayOf(0.0f, 0.45f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, nebPaint1)

            val nebPaint2 = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    w * 0.78f,
                    h * 0.70f,
                    w * 0.50f,
                    intArrayOf(
                        (0x2800BCD4.toLong() and 0x00FFFFFF or (((1.1f - nebulaPulse) * 0x35).toInt().toLong() shl 24)).toInt(),
                        0x08FF9800.toInt(),
                        0x00000000
                    ),
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, nebPaint2)

            // 2. Multi-tier Twinkling Starfield with Spectral Types
            val starPaint = Paint().apply { isAntiAlias = true }
            for (s in stars) {
                val sx = s.relX * w
                val sy = s.relY * h
                val twinkle = (sin(starTwinkle * 3.14159f + s.phase) * 0.35f + 0.65f).coerceIn(0.15f, 1.0f)
                val alpha = (twinkle * 255).toInt()
                starPaint.color = (alpha shl 24) or (s.color.toArgb() and 0x00FFFFFF)
                canvas.drawCircle(sx, sy, s.radius * (0.85f + twinkle * 0.3f), starPaint)

                // 4-Point Cross Diffraction Spikes on Alpha Stars
                if (s.radius > 2.0f && twinkle > 0.6f) {
                    val spikePaint = Paint().apply {
                        isAntiAlias = true
                        color = (alpha / 2 shl 24) or (s.color.toArgb() and 0x00FFFFFF)
                        strokeWidth = 1.0f
                    }
                    val spikeLen = s.radius * 3.8f * twinkle
                    canvas.drawLine(sx - spikeLen, sy, sx + spikeLen, sy, spikePaint)
                    canvas.drawLine(sx, sy - spikeLen, sx, sy + spikeLen, spikePaint)
                }
            }

            // Faint Constellation Lines
            val constPaint = Paint().apply {
                isAntiAlias = true
                color = 0x1A00E5FF.toInt()
                strokeWidth = 1.0f
            }
            if (stars.size >= 8) {
                canvas.drawLine(stars[0].relX * w, stars[0].relY * h, stars[1].relX * w, stars[1].relY * h, constPaint)
                canvas.drawLine(stars[1].relX * w, stars[1].relY * h, stars[4].relX * w, stars[4].relY * h, constPaint)
                canvas.drawLine(stars[4].relX * w, stars[4].relY * h, stars[7].relX * w, stars[7].relY * h, constPaint)
                canvas.drawLine(stars[2].relX * w, stars[2].relY * h, stars[5].relX * w, stars[5].relY * h, constPaint)
                canvas.drawLine(stars[5].relX * w, stars[5].relY * h, stars[8].relX * w, stars[8].relY * h, constPaint)
            }

            // 3. Shooting Stars (Meteors)
            if (showMeteors && meteorProgress <= 1.0f) {
                val startX = w * 0.95f - meteorProgress * w * 0.65f
                val startY = h * 0.05f + meteorProgress * h * 0.45f
                val tailLen = 140f
                val endX = startX + 0.85f * tailLen
                val endY = startY - 0.55f * tailLen

                val meteorPaint = Paint().apply {
                    isAntiAlias = true
                    shader = LinearGradient(
                        startX, startY, endX, endY,
                        0xFFE0F7FA.toInt(),
                        0x0000E5FF.toInt(),
                        Shader.TileMode.CLAMP
                    )
                    strokeWidth = 2.2f
                }
                canvas.drawLine(startX, startY, endX, endY, meteorPaint)
                // Head glow
                val headGlow = Paint().apply {
                    isAntiAlias = true
                    color = 0xFFFFFFFF.toInt()
                }
                canvas.drawCircle(startX, startY, 2.5f, headGlow)
            }

            // 4. Orbital Trajectory Rings
            if (showOrbitalRings) {
                val orbitPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = 0x1800E5FF.toInt()
                    strokeWidth = 1.2f
                }
                val centerX = w * 0.72f
                val centerY = h * 0.50f
                val orbitRadius = min(w, h) * 0.52f
                canvas.drawCircle(centerX, centerY, orbitRadius, orbitPaint)
                canvas.drawCircle(centerX, centerY, orbitRadius * 0.72f, orbitPaint)

                // Orbit node beacon
                val nodeAngle = celestialRotation * 0.01745f
                val nodeX = centerX + cos(nodeAngle) * orbitRadius
                val nodeY = centerY + sin(nodeAngle) * orbitRadius
                val nodePaint = Paint().apply {
                    isAntiAlias = true
                    color = 0xAA00E5FF.toInt()
                }
                canvas.drawCircle(nodeX, nodeY, 4.0f, nodePaint)
                nodePaint.color = 0xFFFFFFFF.toInt()
                canvas.drawCircle(nodeX, nodeY, 1.8f, nodePaint)
            }

            // 5. Large Richly Rendered Celestial Planet
            val pCenterX = w * 0.76f
            val pCenterY = h * 0.48f
            val baseRadius = min(w, h) * 0.28f * planetScale

            drawRealisticPlanet(
                canvas = canvas,
                planet = selectedPlanet,
                cx = pCenterX,
                cy = pCenterY,
                radius = baseRadius,
                rotationDeg = celestialRotation
            )
        }

        // Overlay child content (HUD cards, menus, telemetry)
        content()
    }
}

private fun drawRealisticPlanet(
    canvas: android.graphics.Canvas,
    planet: CelestialPlanet,
    cx: Float,
    cy: Float,
    radius: Float,
    rotationDeg: Float
) {
    val lightAngleRad = -0.75f // Sunlight coming from top-left (approx -43 deg)
    val lightDirX = cos(lightAngleRad)
    val lightDirY = sin(lightAngleRad)

    when (planet) {
        CelestialPlanet.MOON -> {
            // Lunar Outer Corona Glow
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(cx, cy, radius * 1.55f, 0x44B0BEC5.toInt(), 0x00000000, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, radius * 1.55f, glowPaint)

            // Lunar Base Regolith Sphere with 3D Lambertian shading
            val moonPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx + lightDirX * radius * 0.45f,
                    cy + lightDirY * radius * 0.45f,
                    radius * 1.15f,
                    intArrayOf(0xFFECEFF1.toInt(), 0xFF90A4AE.toInt(), 0xFF37474F.toInt(), 0xFF101820.toInt()),
                    floatArrayOf(0.0f, 0.45f, 0.82f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, moonPaint)

            // Dark Basaltic Maria (Sea of Tranquility, Oceanus Procellarum, Mare Serenitatis)
            val mariaPaint = Paint().apply {
                isAntiAlias = true
                color = 0x55263238.toInt()
            }
            canvas.drawCircle(cx - radius * 0.22f, cy - radius * 0.15f, radius * 0.32f, mariaPaint)
            canvas.drawCircle(cx + radius * 0.18f, cy - radius * 0.28f, radius * 0.25f, mariaPaint)
            canvas.drawCircle(cx - radius * 0.05f, cy + radius * 0.25f, radius * 0.38f, mariaPaint)
            canvas.drawCircle(cx + radius * 0.32f, cy + radius * 0.12f, radius * 0.20f, mariaPaint)

            // Prominent Rayed Impact Craters (Tycho, Copernicus, Kepler)
            val craterPaint = Paint().apply { isAntiAlias = true }
            // Tycho (South Crater with radiating bright ejecta rays)
            val tychoX = cx - radius * 0.10f
            val tychoY = cy + radius * 0.55f
            val rayPaint = Paint().apply {
                isAntiAlias = true
                color = 0x44ECEFF1.toInt()
                strokeWidth = 1.5f
            }
            for (ang in 0 until 12) {
                val a = ang * (PI.toFloat() / 6f)
                val ex = tychoX + cos(a) * radius * 0.45f
                val ey = tychoY + sin(a) * radius * 0.45f
                canvas.drawLine(tychoX, tychoY, ex, ey, rayPaint)
            }
            craterPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(tychoX, tychoY, radius * 0.06f, craterPaint)
            craterPaint.color = 0xFF37474F.toInt()
            canvas.drawCircle(tychoX, tychoY, radius * 0.035f, craterPaint)

            // Copernicus Crater
            val copX = cx - radius * 0.25f
            val copY = cy - radius * 0.05f
            craterPaint.color = 0xCCFFFFFF.toInt()
            canvas.drawCircle(copX, copY, radius * 0.08f, craterPaint)
            craterPaint.color = 0xFF263238.toInt()
            canvas.drawCircle(copX, copY, radius * 0.05f, craterPaint)
        }

        CelestialPlanet.MARS -> {
            // Martian Atmospheric Rayleigh Haze (Rust-Orange Halo)
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(cx, cy, radius * 1.50f, 0x55FF5722.toInt(), 0x00000000, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, radius * 1.50f, glowPaint)

            // Martian Desert Surface (Rich Ochre / Rust Iron Oxide)
            val marsPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx + lightDirX * radius * 0.45f,
                    cy + lightDirY * radius * 0.45f,
                    radius * 1.15f,
                    intArrayOf(0xFFFF8A65.toInt(), 0xFFD84315.toInt(), 0xFF8D2A08.toInt(), 0xFF2B0A04.toInt()),
                    floatArrayOf(0.0f, 0.45f, 0.80f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, marsPaint)

            // Valles Marineris Canyon System (Dark rift across the equator)
            val canyonPaint = Paint().apply {
                isAntiAlias = true
                color = 0x664A1005.toInt()
                strokeWidth = radius * 0.08f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(cx - radius * 0.45f, cy + radius * 0.05f, cx + radius * 0.35f, cy + radius * 0.12f, canyonPaint)

            // Olympus Mons Volcanic Caldera & Tharsis Bulge
            val monsPaint = Paint().apply { isAntiAlias = true }
            monsPaint.color = 0x55BF360C.toInt()
            canvas.drawCircle(cx - radius * 0.42f, cy - radius * 0.18f, radius * 0.18f, monsPaint)
            monsPaint.color = 0xFF5D1D06.toInt()
            canvas.drawCircle(cx - radius * 0.42f, cy - radius * 0.18f, radius * 0.06f, monsPaint)

            // Brilliant White Northern Polar Ice Cap (CO₂ & H₂O ice)
            val icePaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    cx, cy - radius, cx, cy - radius * 0.70f,
                    0xFFFFFFFF.toInt(),
                    0x00FFFFFF,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy - radius * 0.85f, radius * 0.28f, icePaint)

            // Phobos Orbiting Close to Mars
            val phobosAng = rotationDeg * 0.05f
            val phobosX = cx + cos(phobosAng) * (radius * 1.35f)
            val phobosY = cy + sin(phobosAng) * (radius * 0.75f)
            val phobosPaint = Paint().apply {
                isAntiAlias = true
                color = 0xFFBCAAA4.toInt()
            }
            canvas.drawCircle(phobosX, phobosY, radius * 0.055f, phobosPaint)
            phobosPaint.color = 0xFF5D4037.toInt()
            canvas.drawCircle(phobosX + 1f, phobosY - 1f, radius * 0.025f, phobosPaint)
        }

        CelestialPlanet.EARTH -> {
            // Earth Brilliant Cyan Atmosphere Halo
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(cx, cy, radius * 1.50f, 0x6640C4FF.toInt(), 0x00000000, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, radius * 1.50f, glowPaint)

            // Earth Deep Sapphire Oceans
            val earthPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx + lightDirX * radius * 0.45f,
                    cy + lightDirY * radius * 0.45f,
                    radius * 1.15f,
                    intArrayOf(0xFF00B0FF.toInt(), 0xFF1565C0.toInt(), 0xFF0D47A1.toInt(), 0xFF021B3A.toInt()),
                    floatArrayOf(0.0f, 0.45f, 0.80f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, earthPaint)

            // Continents (Americas, Eurasia, Africa green & ochre landmasses)
            val landPaint = Paint().apply {
                isAntiAlias = true
                color = 0xDD2E7D32.toInt()
            }
            canvas.drawCircle(cx - radius * 0.20f, cy + radius * 0.10f, radius * 0.28f, landPaint)
            canvas.drawCircle(cx + radius * 0.25f, cy - radius * 0.25f, radius * 0.32f, landPaint)
            landPaint.color = 0xDD689F38.toInt()
            canvas.drawCircle(cx + radius * 0.15f, cy + radius * 0.15f, radius * 0.22f, landPaint)

            // Swirling White Cloud Cover Bands & Cyclones
            val cloudPaint = Paint().apply {
                isAntiAlias = true
                color = 0xCCFFFFFF.toInt()
            }
            canvas.drawCircle(cx - radius * 0.35f, cy - radius * 0.30f, radius * 0.22f, cloudPaint)
            canvas.drawCircle(cx + radius * 0.30f, cy + radius * 0.25f, radius * 0.24f, cloudPaint)
            canvas.drawCircle(cx, cy - radius * 0.05f, radius * 0.18f, cloudPaint)

            // White Polar Ice Cap
            cloudPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, cy - radius * 0.88f, radius * 0.24f, cloudPaint)
        }

        CelestialPlanet.JUPITER -> {
            // Jovian Atmospheric Auroral Glow
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(cx, cy, radius * 1.48f, 0x44FFA000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, radius * 1.48f, glowPaint)

            // Base Planet Disk with 3D shading
            val jupBase = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx + lightDirX * radius * 0.45f,
                    cy + lightDirY * radius * 0.45f,
                    radius * 1.15f,
                    intArrayOf(0xFFFFECB3.toInt(), 0xFFFFD54F.toInt(), 0xFFBF360C.toInt(), 0xFF1B0000.toInt()),
                    floatArrayOf(0.0f, 0.45f, 0.85f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, jupBase)

            // Horizontal Atmospheric Belts & Zones (Alternating amber, cream, and deep ochre)
            val beltPaint = Paint().apply { isAntiAlias = true }
            val belts = listOf(
                Pair(-0.55f, 0x668D6E63),
                Pair(-0.35f, 0x77A1887F),
                Pair(-0.15f, 0x88D7CCC8),
                Pair(0.05f, 0x995D4037),  // South Equatorial Belt
                Pair(0.25f, 0x778D6E63),
                Pair(0.48f, 0x664E342E)
            )
            for ((relY, color) in belts) {
                beltPaint.color = color.toInt()
                val y = cy + relY * radius
                val halfW = sqrt((radius * radius - (relY * radius) * (relY * radius)).coerceAtLeast(0f))
                canvas.drawRect(cx - halfW, y - radius * 0.07f, cx + halfW, y + radius * 0.07f, beltPaint)
            }

            // Great Red Spot with inner vortex eye
            val grsX = cx + radius * 0.28f
            val grsY = cy + radius * 0.18f
            val grsPaint = Paint().apply { isAntiAlias = true }
            grsPaint.color = 0xFFFF5722.toInt()
            canvas.drawOval(android.graphics.RectF(grsX - radius * 0.16f, grsY - radius * 0.09f, grsX + radius * 0.16f, grsY + radius * 0.09f), grsPaint)
            grsPaint.color = 0xFFD84315.toInt()
            canvas.drawOval(android.graphics.RectF(grsX - radius * 0.09f, grsY - radius * 0.05f, grsX + radius * 0.09f, grsY + radius * 0.05f), grsPaint)

            // Galilean Moon (Europa - bright icy cracked sphere)
            val europaAng = rotationDeg * 0.04f + 1.2f
            val europaX = cx + cos(europaAng) * (radius * 1.48f)
            val europaY = cy + sin(europaAng) * (radius * 0.85f)
            val europaPaint = Paint().apply {
                isAntiAlias = true
                color = 0xFFECEFF1.toInt()
            }
            canvas.drawCircle(europaX, europaY, radius * 0.07f, europaPaint)
            europaPaint.color = 0xFF90A4AE.toInt()
            canvas.drawCircle(europaX + 1.5f, europaY - 1f, radius * 0.035f, europaPaint)
        }

        CelestialPlanet.SATURN -> {
            // Saturn Glowing Atmosphere
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(cx, cy, radius * 1.45f, 0x44FFE082.toInt(), 0x00000000, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, radius * 1.45f, glowPaint)

            // Back of Saturn Rings (drawn behind planet)
            drawSaturnRings(canvas, cx, cy, radius, isBack = true)

            // Planet Body (Golden Hued Gas Giant)
            val saturnPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    cx + lightDirX * radius * 0.45f,
                    cy + lightDirY * radius * 0.45f,
                    radius * 1.15f,
                    intArrayOf(0xFFFFF9C4.toInt(), 0xFFFFE082.toInt(), 0xFFFFB74D.toInt(), 0xFF4E342E.toInt()),
                    floatArrayOf(0.0f, 0.45f, 0.85f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, saturnPaint)

            // Soft Cloud Bands across Saturn
            val bandPaint = Paint().apply { isAntiAlias = true }
            for (relY in floatArrayOf(-0.3f, -0.1f, 0.15f, 0.35f)) {
                bandPaint.color = 0x338D6E63.toInt()
                val y = cy + relY * radius
                val halfW = sqrt((radius * radius - (relY * radius) * (relY * radius)).coerceAtLeast(0f))
                canvas.drawRect(cx - halfW, y - radius * 0.05f, cx + halfW, y + radius * 0.05f, bandPaint)
            }

            // Shadow of rings across Saturn's equator
            val ringShadowPaint = Paint().apply {
                isAntiAlias = true
                color = 0x66000000.toInt()
            }
            canvas.drawRect(cx - radius * 0.95f, cy - radius * 0.08f, cx + radius * 0.95f, cy + radius * 0.08f, ringShadowPaint)

            // Front of Saturn Rings (drawn in front of planet)
            drawSaturnRings(canvas, cx, cy, radius, isBack = false)
        }
    }
}

private fun drawSaturnRings(
    canvas: android.graphics.Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    isBack: Boolean
) {
    // Rings are tilted ellipse with Cassini division
    canvas.save()
    canvas.rotate(-22f, cx, cy) // Tilt angle of Saturn's ring plane

    val ringPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // Outer A Ring
    ringPaint.color = if (isBack) 0x66FFF59D.toInt() else 0xCCFFF59D.toInt()
    ringPaint.strokeWidth = radius * 0.22f
    canvas.drawOval(
        android.graphics.RectF(cx - radius * 2.1f, cy - radius * 0.52f, cx + radius * 2.1f, cy + radius * 0.52f),
        ringPaint
    )

    // Cassini Division (Dark gap between A and B ring)
    ringPaint.color = 0x88000000.toInt()
    ringPaint.strokeWidth = radius * 0.05f
    canvas.drawOval(
        android.graphics.RectF(cx - radius * 1.85f, cy - radius * 0.46f, cx + radius * 1.85f, cy + radius * 0.46f),
        ringPaint
    )

    // Inner B Ring (Brighter, denser ice particles)
    ringPaint.color = if (isBack) 0x77FFE082.toInt() else 0xEEFFE082.toInt()
    ringPaint.strokeWidth = radius * 0.26f
    canvas.drawOval(
        android.graphics.RectF(cx - radius * 1.62f, cy - radius * 0.40f, cx + radius * 1.62f, cy + radius * 0.40f),
        ringPaint
    )

    canvas.restore()
}

private data class StarData(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val color: Color,
    val phase: Float
)
