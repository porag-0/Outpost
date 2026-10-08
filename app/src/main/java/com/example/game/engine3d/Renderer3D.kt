package com.example.game.engine3d

import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.*

class Renderer3D {

    private val polygonPath = Path()
    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x55000000.toInt()
    }
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Procedural texture rendering paints (reused to prevent allocations)
    private val hazardPaintYellow = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6.0f
        color = 0xFFFFD600.toInt()
    }
    private val hazardPaintBlack = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6.0f
        color = 0xFF1E1E1E.toInt()
    }
    private val busbarPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xEEFFFFFF.toInt()
    }
    private val cellGridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
        color = 0x6680DEEA.toInt()
    }
    private val rivetPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x88263238.toInt()
    }
    private val rivetHighlightPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x77FFFFFF.toInt()
    }
    private val goldQuiltPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 0.9f
        color = 0x55FFE082.toInt()
    }
    private val glassGlarePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        color = 0x88FFFFFF.toInt()
    }
    private val diffractionPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.1f
        color = 0xAAFFFFFF.toInt()
    }
    private val saturnRingPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // Directional light from Sun in world space
    private val worldSunDir = Vector3(0.45f, 0.75f, 0.48f).normalized()

    // Zero-allocation polygon pool
    private val polygonPool = Array(4500) { ProjectedPolygon() }
    private val activePolygons = ArrayList<ProjectedPolygon>(4500)

    // Reusable vertex buffer to avoid per-mesh list allocations
    private var transformedVertsPool = Array(2048) { Vector3(0f, 0f, 0f) }

    // Reusable clipping buffers
    private val clipInput = arrayOfNulls<Vector3>(4)
    private val clipOutput = Array(8) { Vector3(0f, 0f, 0f) }

    // Cached Shaders & Paints
    private var cachedVignetteWidth = 0f
    private var cachedVignetteHeight = 0f
    private var cachedVignettePaint: Paint? = null
    private var cachedSkyWidth = 0f
    private var cachedSkyHeight = 0f
    private var cachedNebulaPaint1: Paint? = null
    private var cachedNebulaPaint2: Paint? = null
    private var cachedMwPaint: Paint? = null
    private val starPaint = Paint().apply { isAntiAlias = true }

    fun renderScene(
        drawScope: DrawScope,
        cameraPos: Vector3,
        cameraTarget: Vector3,
        meshes: List<Mesh3D>,
        particles: List<Particle3D> = emptyList(),
        flashlightActive: Boolean = true,
        emergencyAlertActive: Boolean = false,
        emergencyPulse: Float = 0f,
        isMars: Boolean = false,
        scannerActive: Boolean = false,
        scannerScanDist: Float = 20f,
        showGroundShadows: Boolean = true,
        showLensFlares: Boolean = true,
        depthFogEnabled: Boolean = true,
        fieldOfView: Float = 56f,
        wireframeHighlight: Boolean = true,
        cinematicPostProcessing: Boolean = true
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0 || height <= 0) return

        val canvas = drawScope.drawContext.canvas.nativeCanvas

        // 1. Draw Space-Themed Cosmic Skybox Background with Stars, Earth, Saturn, Jupiter, Sun & Lens Flare
        drawCelestialAtmosphere(drawScope, width, height, cameraPos, cameraTarget, isMars, emergencyAlertActive, emergencyPulse, showLensFlares)

        // 2. Setup Camera View and Perspective Projection Matrices
        val viewMatrix = Matrix4.lookAt(cameraPos, cameraTarget, Vector3.UP)
        val fovY = fieldOfView * (PI.toFloat() / 180f)
        val aspect = width / height
        val tanHalfFov = tan(fovY * 0.5f)
        val clipNear = 0.08f

        // 3. Draw Contact Shadows for entities on the terrain if enabled
        if (showGroundShadows) {
            drawGroundContactShadows(canvas, viewMatrix, meshes, width, height, aspect, tanHalfFov)
        }

        // 4. Transform, Clip, Cull and Project all Polygons using pre-allocated pool
        activePolygons.clear()
        var poolIndex = 0

        // Convert world sun direction to camera space for accurate dynamic lighting
        val camRotOnly = Matrix4.lookAt(Vector3.ZERO, (cameraTarget - cameraPos).normalized(), Vector3.UP)
        val sunDirCam = camRotOnly.transform(worldSunDir).normalized()

        for (mesh in meshes) {
            val meshPos = mesh.position
            val meshRot = mesh.rotation
            val meshScale = mesh.scale

            val rotYMat = Matrix4.rotationY(meshRot.y)
            val rotXMat = Matrix4.rotationX(meshRot.x)
            val rotZMat = Matrix4.rotationZ(meshRot.z)
            val scaleMat = Matrix4.scale(meshScale.x, meshScale.y, meshScale.z)
            val transMat = Matrix4.translation(meshPos.x, meshPos.y, meshPos.z)

            val modelMatrix = transMat * rotYMat * rotXMat * rotZMat * scaleMat
            val modelViewMatrix = viewMatrix * modelMatrix

            // Frustum Culling on Meshes (except terrain which spans the base)
            if (mesh.name != "Terrain") {
                val centerCam = modelViewMatrix.transform(Vector3.ZERO)
                val meshRadius = when (mesh.name) {
                    "HabitatDome" -> 9.0f
                    "SolarArray" -> 7.5f
                    "SpaceLander" -> 8.5f
                    "Greenhouse" -> 7.5f
                    "OxygenGenerator" -> 5.0f
                    "CommDish" -> 6.0f
                    "CryoStorage" -> 5.0f
                    "ScienceRack" -> 4.0f
                    "Boulder" -> (mesh.scale.x * 2.5f).coerceAtLeast(3.2f)
                    "PerimeterBeaconPylon" -> 2.5f
                    "Astronaut" -> 2.5f
                    "PlanetaryRover" -> 4.0f
                    "ScoutDrone" -> 2.2f
                    else -> 6.5f
                }

                if (centerCam.z > meshRadius + 0.2f) continue
                val zDist = -centerCam.z
                if (zDist > 88f + meshRadius) continue
                val halfW = zDist * aspect * tanHalfFov + meshRadius
                val halfH = zDist * tanHalfFov + meshRadius
                if (abs(centerCam.x) > halfW || abs(centerCam.y) > halfH) continue
            }

            // Ensure vertex pool capacity
            val vSize = mesh.vertices.size
            if (transformedVertsPool.size < vSize) {
                transformedVertsPool = Array((vSize * 1.5f).toInt()) { Vector3(0f, 0f, 0f) }
            }

            for (i in 0 until vSize) {
                transformedVertsPool[i] = modelViewMatrix.transform(mesh.vertices[i])
            }

            for (face in mesh.faces) {
                val p0 = transformedVertsPool[face.v0]
                val p1 = transformedVertsPool[face.v1]
                val p2 = transformedVertsPool[face.v2]
                val isQuad = face.v3 >= 0
                val p3 = if (isQuad) transformedVertsPool[face.v3] else null

                // Early skip if entire polygon is behind near plane
                if (p0.z > -clipNear && p1.z > -clipNear && p2.z > -clipNear && (p3 == null || p3.z > -clipNear)) {
                    continue
                }

                // Early skip if polygon is too far in fog
                if (p0.z < -86f && p1.z < -86f && p2.z < -86f) {
                    continue
                }

                // Face normal in camera space
                val edge1 = p1 - p0
                val edge2 = p2 - p0
                val normal = edge1.cross(edge2).normalized()

                // Backface culling
                val viewDot = normal.dot(p0.normalized())
                if (!face.doubleSided && viewDot > 0.04f) {
                    continue
                }

                if (poolIndex >= polygonPool.size) break

                // Fast non-clipping path: all vertices in front of near plane
                val fullyVisible = p0.z <= -clipNear && p1.z <= -clipNear && p2.z <= -clipNear && (p3 == null || p3.z <= -clipNear)

                if (fullyVisible) {
                    val count = if (isQuad && p3 != null) 4 else 3
                    val avgZ = if (count == 4) (p0.z + p1.z + p2.z + p3!!.z) * 0.25f else (p0.z + p1.z + p2.z) / 3f

                    val finalColor = calculateAdvancedLighting(
                        baseColor = face.baseColor,
                        normal = normal,
                        viewSpacePos = p0,
                        sunDirCam = sunDirCam,
                        emissive = face.emissive,
                        specular = face.specular,
                        flashlightActive = flashlightActive,
                        emergencyAlertActive = emergencyAlertActive,
                        emergencyPulse = emergencyPulse,
                        isMars = isMars,
                        depthDist = -avgZ
                    )

                    val poly = polygonPool[poolIndex++]
                    val xs = poly.xCoords
                    val ys = poly.yCoords
                    poly.count = count
                    poly.averageDepth = if (mesh.name == "Terrain") avgZ - 0.50f else avgZ
                    poly.finalColor = finalColor
                    poly.isEmissive = face.emissive
                    poly.texture = face.texture

                    val invAspectTan = 1f / (aspect * tanHalfFov)
                    val invTan = 1f / tanHalfFov

                    val z0 = -p0.z
                    xs[0] = ((p0.x / z0) * invAspectTan + 1f) * 0.5f * width
                    ys[0] = (1f - (p0.y / z0) * invTan) * 0.5f * height

                    val z1 = -p1.z
                    xs[1] = ((p1.x / z1) * invAspectTan + 1f) * 0.5f * width
                    ys[1] = (1f - (p1.y / z1) * invTan) * 0.5f * height

                    val z2 = -p2.z
                    xs[2] = ((p2.x / z2) * invAspectTan + 1f) * 0.5f * width
                    ys[2] = (1f - (p2.y / z2) * invTan) * 0.5f * height

                    if (count == 4) {
                        val z3 = -p3!!.z
                        xs[3] = ((p3.x / z3) * invAspectTan + 1f) * 0.5f * width
                        ys[3] = (1f - (p3.y / z3) * invTan) * 0.5f * height
                    }

                    activePolygons.add(poly)
                } else {
                    // Partially clipped polygon against near plane
                    clipInput[0] = p0
                    clipInput[1] = p1
                    clipInput[2] = p2
                    val inCount = if (isQuad && p3 != null) {
                        clipInput[3] = p3
                        4
                    } else 3

                    var outCount = 0
                    for (i in 0 until inCount) {
                        val cur = clipInput[i]!!
                        val next = clipInput[(i + 1) % inCount]!!
                        val curIn = cur.z <= -clipNear
                        val nextIn = next.z <= -clipNear

                        if (curIn) {
                            if (nextIn) {
                                if (outCount < clipOutput.size) clipOutput[outCount++] = next
                            } else {
                                val t = (-clipNear - cur.z) / (next.z - cur.z)
                                val ix = cur.x + (next.x - cur.x) * t
                                val iy = cur.y + (next.y - cur.y) * t
                                if (outCount < clipOutput.size) clipOutput[outCount++] = Vector3(ix, iy, -clipNear)
                            }
                        } else {
                            if (nextIn) {
                                val t = (-clipNear - cur.z) / (next.z - cur.z)
                                val ix = cur.x + (next.x - cur.x) * t
                                val iy = cur.y + (next.y - cur.y) * t
                                if (outCount < clipOutput.size) clipOutput[outCount++] = Vector3(ix, iy, -clipNear)
                                if (outCount < clipOutput.size) clipOutput[outCount++] = next
                            }
                        }
                    }

                    if (outCount < 3) continue

                    var sumZ = 0f
                    for (i in 0 until outCount) sumZ += clipOutput[i].z
                    val avgZ = sumZ / outCount

                    val finalColor = calculateAdvancedLighting(
                        baseColor = face.baseColor,
                        normal = normal,
                        viewSpacePos = p0,
                        sunDirCam = sunDirCam,
                        emissive = face.emissive,
                        specular = face.specular,
                        flashlightActive = flashlightActive,
                        emergencyAlertActive = emergencyAlertActive,
                        emergencyPulse = emergencyPulse,
                        isMars = isMars,
                        depthDist = -avgZ
                    )

                    val poly = polygonPool[poolIndex++]
                    val xs = poly.xCoords
                    val ys = poly.yCoords
                    poly.count = outCount
                    poly.averageDepth = if (mesh.name == "Terrain") avgZ - 0.50f else avgZ
                    poly.finalColor = finalColor
                    poly.isEmissive = face.emissive
                    poly.texture = face.texture

                    val invAspectTan = 1f / (aspect * tanHalfFov)
                    val invTan = 1f / tanHalfFov

                    for (i in 0 until outCount) {
                        val pt = clipOutput[i]
                        val zDist = -pt.z
                        xs[i] = ((pt.x / zDist) * invAspectTan + 1f) * 0.5f * width
                        ys[i] = (1f - (pt.y / zDist) * invTan) * 0.5f * height
                    }

                    activePolygons.add(poly)
                }
            }
        }

        // 5. Painter's Algorithm Depth Sort (farthest to nearest)
        activePolygons.sortBy { it.averageDepth }

        // 6. Draw Polygons with Realistic Procedural Surface Textures
        for (poly in activePolygons) {
            polygonPath.reset()
            polygonPath.moveTo(poly.xCoords[0], poly.yCoords[0])
            for (i in 1 until poly.count) {
                polygonPath.lineTo(poly.xCoords[i], poly.yCoords[i])
            }
            polygonPath.close()

            fillPaint.color = poly.finalColor
            canvas.drawPath(polygonPath, fillPaint)

            // High-Realism Procedural Surface Textures (LOD: skip micro-details beyond 30m)
            val isClose = poly.averageDepth > -30f
            when (poly.texture) {
                SurfaceTexture.HAZARD_STRIPES -> {
                    // Authentic Industrial Safety Warning Chevron Stripes
                    strokePaint.color = 0xFF1C1C1C.toInt()
                    strokePaint.strokeWidth = 1.6f
                    canvas.drawPath(polygonPath, strokePaint)

                    if (isClose && poly.count >= 3) {
                        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
                        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
                        for (i in 0 until poly.count) {
                            val px = poly.xCoords[i]; val py = poly.yCoords[i]
                            if (px < minX) minX = px; if (px > maxX) maxX = px
                            if (py < minY) minY = py; if (py > maxY) maxY = py
                        }

                        canvas.save()
                        canvas.clipPath(polygonPath)
                        val stripeSpacing = 12f
                        val diagStart = (minX + minY) - 30f
                        val diagEnd = (maxX + maxY) + 30f
                        var d = diagStart
                        var stripeIdx = 0
                        while (d <= diagEnd) {
                            val p = if (stripeIdx % 2 == 0) hazardPaintYellow else hazardPaintBlack
                            canvas.drawLine(d - maxY, maxY, d - minY, minY, p)
                            d += stripeSpacing
                            stripeIdx++
                        }
                        canvas.restore()
                    }
                }

                SurfaceTexture.SOLAR_CELLS -> {
                    // Deep cosmic blue photovoltaic cells with silver collector busbars & anti-reflective silicon sheen
                    strokePaint.color = 0xAA80DEEA.toInt()
                    strokePaint.strokeWidth = 1.2f
                    canvas.drawPath(polygonPath, strokePaint)

                    if (isClose && poly.count >= 4) {
                        val qx1 = poly.xCoords[0] + (poly.xCoords[1] - poly.xCoords[0]) * 0.33f
                        val qy1 = poly.yCoords[0] + (poly.yCoords[1] - poly.yCoords[0]) * 0.33f
                        val qx2 = poly.xCoords[3] + (poly.xCoords[2] - poly.xCoords[3]) * 0.33f
                        val qy2 = poly.yCoords[3] + (poly.yCoords[2] - poly.yCoords[3]) * 0.33f

                        val rx1 = poly.xCoords[0] + (poly.xCoords[1] - poly.xCoords[0]) * 0.67f
                        val ry1 = poly.yCoords[0] + (poly.yCoords[1] - poly.yCoords[0]) * 0.67f
                        val rx2 = poly.xCoords[3] + (poly.xCoords[2] - poly.xCoords[3]) * 0.67f
                        val ry2 = poly.yCoords[3] + (poly.yCoords[2] - poly.yCoords[3]) * 0.67f

                        canvas.drawLine(qx1, qy1, qx2, qy2, busbarPaint)
                        canvas.drawLine(rx1, ry1, rx2, ry2, busbarPaint)

                        val midX1 = (poly.xCoords[0] + poly.xCoords[3]) * 0.5f
                        val midY1 = (poly.yCoords[0] + poly.yCoords[3]) * 0.5f
                        val midX2 = (poly.xCoords[1] + poly.xCoords[2]) * 0.5f
                        val midY2 = (poly.yCoords[1] + poly.yCoords[2]) * 0.5f
                        canvas.drawLine(midX1, midY1, midX2, midY2, cellGridPaint)
                    }
                }

                SurfaceTexture.METAL_PANELS -> {
                    // Aerospace Titanium Hull Panels with Seam Grooves and Fastener Studs
                    if (isClose) {
                        strokePaint.color = 0x55FFFFFF.toInt()
                        strokePaint.strokeWidth = 1.2f
                        canvas.drawPath(polygonPath, strokePaint)

                        if (poly.averageDepth > -18f) {
                            for (i in 0 until poly.count) {
                                val vx = poly.xCoords[i]; val vy = poly.yCoords[i]
                                canvas.drawCircle(vx, vy, 1.8f, rivetPaint)
                                canvas.drawCircle(vx - 0.5f, vy - 0.5f, 0.7f, rivetHighlightPaint)
                            }
                        }
                    }
                }

                SurfaceTexture.THERMAL_GOLD_FOIL -> {
                    // Apollo/Artemis Multi-Layer Insulation (MLI) Gold Blanket with Crinkled Diamond Quilting
                    if (isClose) {
                        strokePaint.color = 0x66FFD700.toInt()
                        strokePaint.strokeWidth = 1.1f
                        canvas.drawPath(polygonPath, strokePaint)

                        if (poly.count >= 4 && poly.averageDepth > -20f) {
                            canvas.drawLine(poly.xCoords[0], poly.yCoords[0], poly.xCoords[2], poly.yCoords[2], goldQuiltPaint)
                            canvas.drawLine(poly.xCoords[1], poly.yCoords[1], poly.xCoords[3], poly.yCoords[3], goldQuiltPaint)
                        }
                    }
                }

                SurfaceTexture.GREENHOUSE_GLASS -> {
                    // Transparent Aerogel Pane with Glare Reflection Streak
                    strokePaint.color = 0xAA80DEEA.toInt()
                    strokePaint.strokeWidth = 1.4f
                    canvas.drawPath(polygonPath, strokePaint)

                    if (isClose && poly.count >= 4) {
                        val gx1 = poly.xCoords[0] + (poly.xCoords[1] - poly.xCoords[0]) * 0.2f
                        val gy1 = poly.yCoords[0] + (poly.yCoords[1] - poly.yCoords[0]) * 0.2f
                        val gx2 = poly.xCoords[3] + (poly.xCoords[2] - poly.xCoords[3]) * 0.6f
                        val gy2 = poly.yCoords[3] + (poly.yCoords[2] - poly.yCoords[3]) * 0.6f
                        canvas.drawLine(gx1, gy1, gx2, gy2, glassGlarePaint)
                    }
                }

                SurfaceTexture.PAVER_TILES -> {
                    // Sintered Regolith Interlocking Outpost Paver Grid
                    if (isClose) {
                        strokePaint.color = 0x44000000.toInt()
                        strokePaint.strokeWidth = 1.3f
                        canvas.drawPath(polygonPath, strokePaint)
                        if (poly.averageDepth > -16f && poly.count >= 4) {
                            for (i in 0 until poly.count) {
                                canvas.drawCircle(poly.xCoords[i], poly.yCoords[i], 1.2f, rivetPaint)
                            }
                        }
                    }
                }

                SurfaceTexture.REGOLITH_DUST -> {
                    // Planetary Regolith Micro-Texture & Contour Shading
                    if (wireframeHighlight && poly.averageDepth > -22f) {
                        strokePaint.color = ((poly.finalColor and 0x00FFFFFF) or 0x1A000000)
                        strokePaint.strokeWidth = 0.8f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }

                SurfaceTexture.CRATER_ROCK -> {
                    // Basalt/Anorthosite Mineral Fractures
                    if (isClose) {
                        strokePaint.color = 0x55000000.toInt()
                        strokePaint.strokeWidth = 1.2f
                        canvas.drawPath(polygonPath, strokePaint)
                        if (poly.count >= 3 && poly.averageDepth > -18f) {
                            val cx = (poly.xCoords[0] + poly.xCoords[1] + poly.xCoords[2]) / 3f
                            val cy = (poly.yCoords[0] + poly.yCoords[1] + poly.yCoords[2]) / 3f
                            canvas.drawLine(poly.xCoords[0], poly.yCoords[0], cx, cy, strokePaint)
                        }
                    }
                }

                SurfaceTexture.NONE -> {
                    if (wireframeHighlight && !poly.isEmissive && poly.averageDepth > -24f) {
                        strokePaint.color = (poly.finalColor and 0x00FFFFFF) or 0x1A000000
                        strokePaint.strokeWidth = 0.9f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }
            }
        }

        // 7. Advanced 3D Particle Plumes & Atmospheric Swirls
        drawAdvancedParticles(canvas, particles, viewMatrix, width, height, aspect, tanHalfFov)

        // 8. 3D Holographic Terrain Scanner Ring
        if (scannerActive) {
            drawHolographicScannerRadar(canvas, width, height, cameraPos, cameraTarget)
        }

        // 9. Cinematic Visor Vignette Post-Processing
        if (cinematicPostProcessing) {
            drawCinematicVisorVignette(canvas, width, height)
        }
    }

    private fun drawCinematicVisorVignette(canvas: android.graphics.Canvas, width: Float, height: Float) {
        if (cachedVignettePaint == null || cachedVignetteWidth != width || cachedVignetteHeight != height) {
            cachedVignetteWidth = width
            cachedVignetteHeight = height
            cachedVignettePaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    width * 0.5f,
                    height * 0.5f,
                    kotlin.math.hypot(width * 0.5f, height * 0.5f),
                    0x00000000.toInt(),
                    0x88000000.toInt(),
                    Shader.TileMode.CLAMP
                )
            }
        }
        cachedVignettePaint?.let { canvas.drawRect(0f, 0f, width, height, it) }
    }

    private fun drawGroundContactShadows(
        canvas: android.graphics.Canvas,
        viewMatrix: Matrix4,
        meshes: List<Mesh3D>,
        width: Float,
        height: Float,
        aspect: Float,
        tanHalfFov: Float
    ) {
        for (mesh in meshes) {
            if (mesh.name == "Astronaut" || mesh.name == "PlanetaryRover" || mesh.name == "SpaceLander" ||
                mesh.name == "BeaconPylon" || mesh.name == "CryoStorage" || mesh.name == "ScienceRack" ||
                mesh.name == "ScoutDrone"
            ) {
                val groundY = MeshFactory.getTerrainHeight(mesh.position.x, mesh.position.z)
                val shadowOffset = Vector3(-worldSunDir.x * 0.35f, 0f, -worldSunDir.z * 0.35f)
                val shadowPosWorld = Vector3(mesh.position.x + shadowOffset.x, groundY + 0.03f, mesh.position.z + shadowOffset.z)
                val shadowPosView = viewMatrix.transform(shadowPosWorld)
                if (shadowPosView.z >= -0.5f) continue

                val zDist = -shadowPosView.z
                val projX = shadowPosView.x / (zDist * aspect * tanHalfFov)
                val projY = shadowPosView.y / (zDist * tanHalfFov)

                val screenX = (projX + 1f) * 0.5f * width
                val screenY = (1f - projY) * 0.5f * height

                val radius = when (mesh.name) {
                    "SpaceLander" -> (3.6f / zDist * 280f)
                    "PlanetaryRover" -> (2.0f / zDist * 280f)
                    "CryoStorage" -> (1.6f / zDist * 280f)
                    "ScienceRack" -> (1.2f / zDist * 280f)
                    "BeaconPylon" -> (0.8f / zDist * 280f)
                    "ScoutDrone" -> (0.7f / zDist * 280f)
                    else -> (0.65f / zDist * 280f)
                }.coerceIn(3f, 95f)

                canvas.drawOval(
                    screenX - radius,
                    screenY - radius * 0.42f,
                    screenX + radius,
                    screenY + radius * 0.42f,
                    shadowPaint
                )
            }
        }
    }

    private fun calculateAdvancedLighting(
        baseColor: Long,
        normal: Vector3,
        viewSpacePos: Vector3,
        sunDirCam: Vector3,
        emissive: Boolean,
        specular: Float,
        flashlightActive: Boolean,
        emergencyAlertActive: Boolean,
        emergencyPulse: Float,
        isMars: Boolean,
        depthDist: Float
    ): Int {
        if (emissive) return baseColor.toInt()

        val a = ((baseColor shr 24) and 0xFF).toInt()
        var r = ((baseColor shr 16) and 0xFF).toFloat()
        var g = ((baseColor shr 8) and 0xFF).toFloat()
        var b = (baseColor and 0xFF).toFloat()

        // Directional Sun lighting (harsh vacuum space shadows)
        val nDotL = normal.dot(sunDirCam).coerceIn(0f, 1f)

        // Subtle secondary ambient bounce from planetary regolith
        val ambientBase = if (isMars) 0.36f else 0.22f
        var lightFactor = ambientBase + nDotL * 0.78f

        // Planetary Opposition Surge (Heiligenschein / retroreflection)
        // Regolith dust grains reflect intense light back toward direct solar line-of-sight
        val viewDir = (-viewSpacePos).normalized()
        val oppositionCos = viewDir.dot(sunDirCam).coerceIn(0f, 1f)
        if (oppositionCos > 0.84f) {
            val surge = (oppositionCos - 0.84f) / 0.16f
            lightFactor += surge * surge * 0.25f
        }

        // Helmet mounted spotlight beam
        if (flashlightActive) {
            val dist = viewSpacePos.length()
            val forward = Vector3(0f, 0f, -1f)
            val spotDot = viewSpacePos.normalized().dot(forward).coerceIn(0f, 1f)
            if (spotDot > 0.60f && dist < 42f) {
                val spotIntensity = ((spotDot - 0.60f) / 0.40f) * (1f - dist / 42f)
                lightFactor += spotIntensity * 0.55f
            }
        }

        // Specular highlight / Fresnel visor shine
        if (specular > 0.05f && nDotL > 0.15f) {
            val halfVector = (sunDirCam + viewDir).normalized()
            val nDotH = normal.dot(halfVector).coerceIn(0f, 1f)
            val specPower = if (specular > 0.6f) 32.0 else 12.0
            val spec = Math.pow(nDotH.toDouble(), specPower).toFloat() * specular * 1.5f

            // Visor specular boost
            r = (r + spec * 220f).coerceAtMost(255f)
            g = (g + spec * 220f).coerceAtMost(255f)
            b = (b + spec * 220f).coerceAtMost(255f)
        }

        r = (r * lightFactor).coerceIn(0f, 255f)
        g = (g * lightFactor).coerceIn(0f, 255f)
        b = (b * lightFactor).coerceIn(0f, 255f)

        // Depth fogging (Atmospheric perspective / space darkness)
        val fogStart = 22f
        val fogEnd = 88f
        if (depthDist > fogStart) {
            val fogFactor = ((depthDist - fogStart) / (fogEnd - fogStart)).coerceIn(0f, 0.72f)
            val fogR = if (isMars) 90f else 8f
            val fogG = if (isMars) 32f else 12f
            val fogB = if (isMars) 18f else 18f

            r = r * (1f - fogFactor) + fogR * fogFactor
            g = g * (1f - fogFactor) + fogG * fogFactor
            b = b * (1f - fogFactor) + fogB * fogFactor
        }

        // Emergency strobe alert
        if (emergencyAlertActive && emergencyPulse > 0.05f) {
            r = (r + emergencyPulse * 85f).coerceAtMost(255f)
            g = (g * (1f - emergencyPulse * 0.45f)).coerceIn(0f, 255f)
            b = (b * (1f - emergencyPulse * 0.45f)).coerceIn(0f, 255f)
        }

        return (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    private fun drawCelestialAtmosphere(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        cameraPos: Vector3,
        cameraTarget: Vector3,
        isMars: Boolean,
        emergencyAlertActive: Boolean,
        emergencyPulse: Float,
        showLensFlares: Boolean
    ) {
        val canvas = drawScope.drawContext.canvas.nativeCanvas
        val camDir = (cameraTarget - cameraPos).normalized()
        val horizonY = (height * 0.5f) - (camDir.y * height * 0.85f)
        val camYaw = kotlin.math.atan2(camDir.x, camDir.z)
        val fovHorizontal = (56f * PI.toFloat() / 180f) * (width / height)

        // Helper to project 360-degree celestial azimuth and vertical elevation into screen space with smooth wrapping
        class CelestialPos(val x: Float, val y: Float)

        fun projectCelestial(azimuthRad: Float, elevNorm: Float): CelestialPos? {
            var diff = (azimuthRad - camYaw) % (2f * PI.toFloat())
            if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
            if (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
            if (abs(diff) > fovHorizontal * 0.82f) return null
            val sx = width * 0.5f + (diff / fovHorizontal) * width
            val sy = horizonY - (height * elevNorm)
            return CelestialPos(sx, sy)
        }

        // 1. Base Deep Space Void Canvas
        val skyTopColor = if (emergencyAlertActive) 0xFF2A0604.toInt() else 0xFF01040A.toInt()
        val skyBottomColor = if (isMars) {
            if (emergencyAlertActive) 0xFF6D1B0B.toInt() else 0xFF4A1F12.toInt()
        } else {
            0xFF050B18.toInt()
        }
        fillPaint.shader = LinearGradient(0f, 0f, 0f, horizonY.coerceAtLeast(30f), skyTopColor, skyBottomColor, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width, height, fillPaint)
        fillPaint.shader = null

        // 2. Cosmic Nebulae (Volumetric stellar nurseries in Carina, Cygnus & Milky Way)
        val animTimeSec = System.currentTimeMillis() * 0.001f

        // Purple/Magenta Carina Nebula
        val carinaOffset = projectCelestial(0.85f, 0.45f)
        if (carinaOffset != null && carinaOffset.y < horizonY) {
            glowPaint.shader = RadialGradient(
                carinaOffset.x, carinaOffset.y, width * 0.38f,
                0x338E24AA.toInt(),
                0x00000000.toInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(carinaOffset.x, carinaOffset.y, width * 0.38f, glowPaint)
            glowPaint.shader = null
        }

        // Electric Teal/Cyan Cygnus Nebula
        val cygnusOffset = projectCelestial(-1.65f, 0.50f)
        if (cygnusOffset != null && cygnusOffset.y < horizonY) {
            glowPaint.shader = RadialGradient(
                cygnusOffset.x, cygnusOffset.y, width * 0.34f,
                0x2800E5FF.toInt(),
                0x00000000.toInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cygnusOffset.x, cygnusOffset.y, width * 0.34f, glowPaint)
            glowPaint.shader = null
        }

        // Milky Way Star Lane ribbon across the celestial canopy
        val mwOffset = projectCelestial(-0.20f, 0.40f)
        if (mwOffset != null) {
            glowPaint.shader = LinearGradient(
                mwOffset.x - width * 0.4f, mwOffset.y - height * 0.3f,
                mwOffset.x + width * 0.4f, mwOffset.y + height * 0.3f,
                0x1EFFF8E1.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width, horizonY.coerceAtLeast(30f), glowPaint)
            glowPaint.shader = null
        }

        // Martian lower atmospheric haze layer (if on Mars)
        if (isMars) {
            fillPaint.color = 0x558D4320.toInt()
            canvas.drawRect(0f, (horizonY - 55f).coerceAtLeast(0f), width, (horizonY + 45f).coerceAtMost(height), fillPaint)
        }

        // 3. Dense 360-Degree Starfield with Parallax
        val starCount = 280
        for (i in 0 until starCount) {
            val starAzimuth = ((i * 0.02244f * PI.toFloat()) % (2f * PI.toFloat())) - PI.toFloat()
            val starElev = 0.05f + ((sin(i * 37.19f) * 0.5f + 0.5f) * 0.75f)
            val sPos = projectCelestial(starAzimuth, starElev) ?: continue
            if (sPos.y < horizonY) {
                val twinkle = (sin(i * 2.87f + animTimeSec * 2.2f) * 0.35f + 0.65f)
                val alpha = (twinkle * 255).toInt().coerceIn(40, 255)

                val starColorRgb = when (i % 7) {
                    0, 4 -> 0x0080D8FF // Blue Giant
                    1 -> 0x00FFF9C4    // Solar Yellow
                    2 -> 0x00FF8A80    // Red Dwarf
                    3 -> 0x00B388FF    // Violet-White Pulsar
                    else -> 0x00FFFFFF // Pure White
                }
                starPaint.color = (alpha shl 24) or starColorRgb
                val starSize = when {
                    i % 23 == 0 -> 2.6f
                    i % 9 == 0 -> 1.8f
                    i % 4 == 0 -> 1.3f
                    else -> 0.85f
                }
                canvas.drawCircle(sPos.x, sPos.y, starSize, starPaint)

                // 4-point cross diffraction spikes on brightest alpha stars
                if (i % 23 == 0 && sPos.y < horizonY - 15f) {
                    val spikeLen = 6.5f * twinkle
                    diffractionPaint.color = (alpha shl 24) or 0x00FFFFFF
                    canvas.drawLine(sPos.x - spikeLen, sPos.y, sPos.x + spikeLen, sPos.y, diffractionPaint)
                    canvas.drawLine(sPos.x, sPos.y - spikeLen, sPos.x, sPos.y + spikeLen, diffractionPaint)
                }
            }
        }

        // Dynamic Shooting Star / Meteor Streak
        val meteorCycle = (animTimeSec * 0.32f) % 4.0f
        if (meteorCycle < 0.85f) {
            val mProgress = meteorCycle / 0.85f
            val startM = projectCelestial(0.15f, 0.65f)
            if (startM != null) {
                val mStartX = startM.x + mProgress * 180f
                val mStartY = startM.y + mProgress * 110f
                val mEndX = mStartX - 55f
                val mEndY = mStartY - 34f
                val meteorAlpha = ((1f - mProgress) * 220).toInt().coerceIn(0, 255)
                strokePaint.color = (meteorAlpha shl 24) or 0x00E0F7FA
                strokePaint.strokeWidth = 2.2f
                canvas.drawLine(mStartX, mStartY, mEndX, mEndY, strokePaint)
                fillPaint.color = (meteorAlpha shl 24) or 0x00FFFFFF
                canvas.drawCircle(mStartX, mStartY, 2.5f, fillPaint)
            }
        }

        // 4. PLANET 1: THE BLUE MARBLE (EARTH)
        val earthPos = projectCelestial(0.48f, 0.40f)
        if (earthPos != null && earthPos.y < horizonY + 30f) {
            val ex = earthPos.x
            val ey = earthPos.y
            val earthRadius = 38f

            // Multi-layered Rayleigh Atmospheric Scattering Glow
            glowPaint.shader = RadialGradient(ex, ey, earthRadius * 1.55f, 0x6640C4FF.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(ex, ey, earthRadius * 1.55f, glowPaint)
            glowPaint.shader = RadialGradient(ex, ey, earthRadius * 1.18f, 0xAA00E5FF.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(ex, ey, earthRadius * 1.18f, glowPaint)
            glowPaint.shader = null

            // Deep Royal Blue Oceans
            fillPaint.color = 0xFF0D47A1.toInt()
            canvas.drawCircle(ex, ey, earthRadius, fillPaint)

            // Continents: Green forests, tan deserts, and polar ice
            fillPaint.color = 0xFF2E7D32.toInt()
            canvas.drawCircle(ex - 8f, ey + 4f, 16f, fillPaint)
            canvas.drawCircle(ex + 11f, ey - 6f, 13f, fillPaint)
            canvas.drawCircle(ex - 12f, ey - 10f, 11f, fillPaint)
            fillPaint.color = 0xFFC5A059.toInt()
            canvas.drawCircle(ex - 5f, ey - 1f, 9f, fillPaint)
            canvas.drawCircle(ex + 15f, ey + 10f, 7f, fillPaint)
            fillPaint.color = 0xFFECEFF1.toInt()
            canvas.drawCircle(ex, ey - earthRadius + 6f, 10f, fillPaint)

            // Swirling Realistic White Cyclonic Weather Bands
            fillPaint.color = 0xDDFFFFFF.toInt()
            canvas.drawCircle(ex + 12f, ey - 10f, 12f, fillPaint)
            canvas.drawCircle(ex - 7f, ey + 13f, 11f, fillPaint)
            canvas.drawCircle(ex + 3f, ey + 2f, 8f, fillPaint)
            canvas.drawOval(RectF(ex - 22f, ey - 6f, ex + 20f, ey + 2f), fillPaint)

            // 3D Spherical Day/Night Terminator Crescent Shadow
            glowPaint.shader = RadialGradient(ex + 16f, ey + 11f, earthRadius * 1.05f, 0x00000000.toInt(), 0xBB000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(ex, ey, earthRadius, glowPaint)
            glowPaint.shader = null

            // Gateway Spacedock / ISS Orbital Telemetry Beacon
            val beaconAngle = animTimeSec * 1.2f
            val bx = ex + cos(beaconAngle) * (earthRadius + 9f)
            val by = ey + sin(beaconAngle) * (earthRadius + 6f)
            fillPaint.color = 0xFF00E676.toInt()
            canvas.drawCircle(bx, by, 1.8f, fillPaint)
            strokePaint.color = 0x8800E5FF.toInt()
            strokePaint.strokeWidth = 1.0f
            canvas.drawCircle(bx, by, 4.0f, strokePaint)
        }

        // 5. PLANET 2: THE RINGED SOVEREIGN (SATURN)
        val saturnPos = projectCelestial(-1.25f, 0.44f)
        if (saturnPos != null && saturnPos.y < horizonY + 30f) {
            val sx = saturnPos.x
            val sy = saturnPos.y
            val saturnR = 14f

            // Tilted Concentric Rings (drawn behind planet first)
            canvas.save()
            canvas.rotate(-18f, sx, sy)

            // Outer A-Ring
            saturnRingPaint.color = 0xAAECD9BA.toInt()
            saturnRingPaint.strokeWidth = 4.2f
            canvas.drawOval(RectF(sx - 48f, sy - 12f, sx + 48f, sy + 12f), saturnRingPaint)

            // Cassini Division dark gap
            saturnRingPaint.color = 0x44261C14.toInt()
            saturnRingPaint.strokeWidth = 1.8f
            canvas.drawOval(RectF(sx - 42f, sy - 10.5f, sx + 42f, sy + 10.5f), saturnRingPaint)

            // Bright Broad B-Ring
            saturnRingPaint.color = 0xEEFFF3E0.toInt()
            saturnRingPaint.strokeWidth = 6.0f
            canvas.drawOval(RectF(sx - 36f, sy - 9f, sx + 36f, sy + 9f), saturnRingPaint)

            // Inner Translucent Crepe C-Ring
            saturnRingPaint.color = 0x558D6E63.toInt()
            saturnRingPaint.strokeWidth = 2.5f
            canvas.drawOval(RectF(sx - 24f, sy - 6f, sx + 24f, sy + 6f), saturnRingPaint)

            canvas.restore()

            // Saturn Gas Giant Globe
            fillPaint.color = 0xFFFFE082.toInt()
            canvas.drawCircle(sx, sy, saturnR, fillPaint)

            // Atmospheric latitudinal cloud bands
            fillPaint.color = 0xFFD7CCC8.toInt()
            canvas.drawRect(sx - saturnR * 0.95f, sy - 4f, sx + saturnR * 0.95f, sy - 1.5f, fillPaint)
            fillPaint.color = 0xFFFFB74D.toInt()
            canvas.drawRect(sx - saturnR * 0.95f, sy + 1f, sx + saturnR * 0.95f, sy + 3.8f, fillPaint)

            // Tilted Concentric Rings (front arc over planet)
            canvas.save()
            canvas.rotate(-18f, sx, sy)
            saturnRingPaint.color = 0xDDECD9BA.toInt()
            saturnRingPaint.strokeWidth = 4.2f
            canvas.drawArc(RectF(sx - 48f, sy - 12f, sx + 48f, sy + 12f), 0f, 180f, false, saturnRingPaint)
            saturnRingPaint.color = 0xFFFFF3E0.toInt()
            saturnRingPaint.strokeWidth = 6.0f
            canvas.drawArc(RectF(sx - 36f, sy - 9f, sx + 36f, sy + 9f), 0f, 180f, false, saturnRingPaint)
            canvas.restore()

            // Titan Moon (golden haze)
            fillPaint.color = 0xFFFFD54F.toInt()
            canvas.drawCircle(sx + 56f, sy - 18f, 2.4f, fillPaint)
            strokePaint.color = 0x66FFB300.toInt()
            strokePaint.strokeWidth = 1f
            canvas.drawCircle(sx + 56f, sy - 18f, 4.5f, strokePaint)
        }

        // 6. PLANET 3: THE JOVIAN COLOSSUS (JUPITER)
        val jupPos = projectCelestial(2.15f, 0.35f)
        if (jupPos != null && jupPos.y < horizonY + 30f) {
            val jx = jupPos.x
            val jy = jupPos.y
            val jupR = 22f

            // Soft atmospheric glow
            glowPaint.shader = RadialGradient(jx, jy, jupR * 1.35f, 0x44FFE082.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(jx, jy, jupR * 1.35f, glowPaint)
            glowPaint.shader = null

            // Jupiter Base Globe (warm cream)
            fillPaint.color = 0xFFFFF8E1.toInt()
            canvas.drawCircle(jx, jy, jupR, fillPaint)

            // Turbulent Equatorial & Temperate Cloud Belts
            fillPaint.color = 0xFF8D6E63.toInt()
            canvas.drawRect(jx - jupR * 0.95f, jy - 7f, jx + jupR * 0.95f, jy - 3f, fillPaint)
            fillPaint.color = 0xFFA1887F.toInt()
            canvas.drawRect(jx - jupR * 0.95f, jy + 2f, jx + jupR * 0.95f, jy + 6.5f, fillPaint)
            fillPaint.color = 0xFFBCAAA4.toInt()
            canvas.drawRect(jx - jupR * 0.92f, jy + 10f, jx + jupR * 0.92f, jy + 13.5f, fillPaint)
            canvas.drawRect(jx - jupR * 0.92f, jy - 14f, jx + jupR * 0.92f, jy - 11f, fillPaint)

            // Great Red Spot Oval Storm Vortex
            fillPaint.color = 0xFFFF5722.toInt()
            canvas.drawOval(RectF(jx + 5f, jy + 2f, jx + 15f, jy + 8f), fillPaint)
            fillPaint.color = 0xFFD84315.toInt()
            canvas.drawCircle(jx + 10f, jy + 5f, 2.0f, fillPaint)

            // Galilean Moons in orbital plane
            // Europa (icy brilliant white with cyan aura)
            fillPaint.color = 0xFFE0F7FA.toInt()
            canvas.drawCircle(jx - 38f, jy - 2f, 2.2f, fillPaint)
            strokePaint.color = 0x6600E5FF.toInt()
            strokePaint.strokeWidth = 1f
            canvas.drawCircle(jx - 38f, jy - 2f, 4.2f, strokePaint)

            // Io (volcanic sulfur gold)
            fillPaint.color = 0xFFFFD54F.toInt()
            canvas.drawCircle(jx + 42f, jy + 4f, 2.4f, fillPaint)

            // Ganymede (slate grey)
            fillPaint.color = 0xFFB0BEC5.toInt()
            canvas.drawCircle(jx - 54f, jy - 5f, 3.0f, fillPaint)

            // Callisto (cratered charcoal)
            fillPaint.color = 0xFF78909C.toInt()
            canvas.drawCircle(jx + 68f, jy + 7f, 2.8f, fillPaint)
        }

        // 7. PLANET 4: MARS / LUNA MOON COMPANION
        val companionPos = projectCelestial(-2.45f, 0.46f)
        if (companionPos != null && companionPos.y < horizonY + 30f) {
            val cx = companionPos.x
            val cy = companionPos.y

            if (!isMars) {
                // When on Moon base: View Mars as a glowing red celestial neighbor
                val marsR = 17f
                glowPaint.shader = RadialGradient(cx, cy, marsR * 1.45f, 0x66FF5722.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, marsR * 1.45f, glowPaint)
                glowPaint.shader = null

                fillPaint.color = 0xFFBF360C.toInt()
                canvas.drawCircle(cx, cy, marsR, fillPaint)

                // Dark volcanic maria (Syrtis Major)
                fillPaint.color = 0xFF4E1605.toInt()
                canvas.drawCircle(cx - 3f, cy + 2f, 8f, fillPaint)
                canvas.drawCircle(cx + 6f, cy - 3f, 6f, fillPaint)

                // Brilliant White Polar Ice Cap
                fillPaint.color = 0xFFFFFFFF.toInt()
                canvas.drawCircle(cx, cy - marsR + 3.5f, 5.5f, fillPaint)
            } else {
                // When on Mars base: View Luna (The Moon) + Phobos & Deimos
                val moonR = 15f
                fillPaint.color = 0xFFB0BEC5.toInt()
                canvas.drawCircle(cx, cy, moonR, fillPaint)
                // Dark lunar maria
                fillPaint.color = 0xFF546E7A.toInt()
                canvas.drawCircle(cx - 3f, cy - 2f, 6f, fillPaint)
                canvas.drawCircle(cx + 4f, cy + 3f, 5f, fillPaint)

                // Phobos & Deimos
                fillPaint.color = 0xFF8D6E63.toInt()
                canvas.drawCircle(cx + 28f, cy - 14f, 4.0f, fillPaint)
                fillPaint.color = 0xFFA1887F.toInt()
                canvas.drawCircle(cx - 36f, cy + 18f, 2.5f, fillPaint)
            }
        }

        // 8. Distant Horizon Mountain Ridges & Crater Rims
        drawDistantPlanetaryHorizon(canvas, width, height, horizonY, camYaw, isMars, emergencyAlertActive)

        // 9. THE RADIANT STAR / SUN with Anamorphic Coronal Bloom
        val sunPos = projectCelestial(-0.55f, 0.28f)
        if (sunPos != null && sunPos.y < horizonY) {
            val sunX = sunPos.x
            val sunY = sunPos.y

            // Multi-layered Solar Corona Halo
            val coronaRadius = if (emergencyAlertActive) 110f else 95f
            val coronaColor = if (emergencyAlertActive) 0x66FF1744.toInt() else 0x55FFF59D.toInt()
            glowPaint.shader = RadialGradient(sunX, sunY, coronaRadius, coronaColor, 0x00000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(sunX, sunY, coronaRadius, glowPaint)
            glowPaint.shader = RadialGradient(sunX, sunY, coronaRadius * 0.45f, 0xAAFFFFFF.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(sunX, sunY, coronaRadius * 0.45f, glowPaint)
            glowPaint.shader = null

            // Brilliant White-Hot Solar Core
            fillPaint.color = 0xFFFFFFF0.toInt()
            canvas.drawCircle(sunX, sunY, 17f, fillPaint)

            // Radiant 6-point Diffraction Starburst Rays
            strokePaint.color = if (emergencyAlertActive) 0x77FF5252.toInt() else 0x88FFF9C4.toInt()
            strokePaint.strokeWidth = 1.4f
            for (rayAngle in listOf(0f, 60f, 120f)) {
                val rad = rayAngle * (PI.toFloat() / 180f)
                val rayLen = 50f
                val dx = cos(rad) * rayLen
                val dy = sin(rad) * rayLen
                canvas.drawLine(sunX - dx, sunY - dy, sunX + dx, sunY + dy, strokePaint)
            }

            // Anamorphic Visor Optical Bloom Flare Streak & Rainbow Aperture Disks
            if (showLensFlares) {
                val flarePaint = Paint().apply {
                    strokeWidth = 2.2f
                    color = if (emergencyAlertActive) 0x66FF5252.toInt() else 0x5500E5FF.toInt()
                }
                canvas.drawLine(sunX - 220f, sunY, sunX + 220f, sunY, flarePaint)
                canvas.drawCircle(sunX * 1.25f, sunY * 1.2f, 13f, flarePaint)
                canvas.drawCircle(sunX * 1.55f, sunY * 1.45f, 8f, flarePaint)
            }
        }
    }

    private fun drawDistantPlanetaryHorizon(
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float,
        horizonY: Float,
        yawAngle: Float,
        isMars: Boolean,
        emergencyAlertActive: Boolean
    ) {
        val pathFar = Path()
        val pathNear = Path()

        val steps = 20
        val stepW = width / steps

        // Far Mountain Layer
        pathFar.moveTo(0f, height)
        pathFar.lineTo(0f, horizonY)
        for (i in 0..steps) {
            val px = i * stepW
            val freq = (i.toFloat() / steps) * 4f * Math.PI.toFloat() + yawAngle * 1.5f
            val hOffset = sin(freq).toFloat() * 24f + cos(freq * 2.2f).toFloat() * 12f
            pathFar.lineTo(px, horizonY - 16f - hOffset)
        }
        pathFar.lineTo(width, height)
        pathFar.close()

        val farColor = if (isMars) {
            if (emergencyAlertActive) 0xFF4A1208.toInt() else 0xFF421E12.toInt()
        } else {
            0xFF0D1117.toInt()
        }
        val farPaint = Paint().apply { isAntiAlias = true; color = farColor }
        canvas.drawPath(pathFar, farPaint)

        // Near Crater Rim / Canyon Ridge Layer
        pathNear.moveTo(0f, height)
        pathNear.lineTo(0f, horizonY)
        for (i in 0..steps) {
            val px = i * stepW
            val freq = (i.toFloat() / steps) * 5.5f * Math.PI.toFloat() + yawAngle * 2.4f
            val hOffset = sin(freq + 0.8f).toFloat() * 18f + cos(freq * 1.6f).toFloat() * 9f
            pathNear.lineTo(px, horizonY - 8f - hOffset)
        }
        pathNear.lineTo(width, height)
        pathNear.close()

        val nearColor = if (isMars) {
            if (emergencyAlertActive) 0xFF6B1D0E.toInt() else 0xFF632B18.toInt()
        } else {
            0xFF1A1F26.toInt()
        }
        val nearPaint = Paint().apply { isAntiAlias = true; color = nearColor }
        canvas.drawPath(pathNear, nearPaint)

        // Subtle crest rim highlight along the rocky peaks
        val rimStroke = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = if (isMars) 0x55FF8A65.toInt() else 0x4490A4AE.toInt()
        }
        canvas.drawPath(pathNear, rimStroke)
    }

    private fun drawAdvancedParticles(
        canvas: android.graphics.Canvas,
        particles: List<Particle3D>,
        viewMatrix: Matrix4,
        width: Float,
        height: Float,
        aspect: Float,
        tanHalfFov: Float
    ) {
        if (particles.isEmpty()) return

        val pPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

        for (p in particles) {
            val viewPos = viewMatrix.transform(p.position)
            if (viewPos.z >= -0.2f) continue

            val zDist = -viewPos.z
            val projX = viewPos.x / (zDist * aspect * tanHalfFov)
            val projY = viewPos.y / (zDist * tanHalfFov)

            val screenX = (projX + 1f) * 0.5f * width
            val screenY = (1f - projY) * 0.5f * height

            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            val baseColorInt = p.color.toInt()
            val a = (((baseColorInt shr 24) and 0xFF) * alpha).toInt()
            pPaint.color = (a shl 24) or (baseColorInt and 0x00FFFFFF)

            val radius = (p.size / zDist * 220f).coerceIn(2f, 26f)
            canvas.drawCircle(screenX, screenY, radius, pPaint)
        }
    }

    private fun drawHolographicScannerRadar(
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float,
        cameraPos: Vector3,
        cameraTarget: Vector3
    ) {
        val sweepPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xCC00E5FF.toInt()
        }

        val cx = width * 0.5f
        val cy = height * 0.65f

        val animPulse = (System.currentTimeMillis() % 1600L) / 1600f
        val pulseRadius1 = animPulse * 160f
        val pulseRadius2 = ((animPulse + 0.5f) % 1.0f) * 160f

        sweepPaint.alpha = ((1f - animPulse) * 220).toInt()
        canvas.drawCircle(cx, cy, pulseRadius1, sweepPaint)

        sweepPaint.alpha = (((1f - (pulseRadius2 / 160f))) * 220).toInt()
        canvas.drawCircle(cx, cy, pulseRadius2, sweepPaint)

        sweepPaint.alpha = 180
        sweepPaint.strokeWidth = 1.5f
        canvas.drawLine(cx - 30f, cy, cx + 30f, cy, sweepPaint)
        canvas.drawLine(cx, cy - 30f, cx, cy + 30f, sweepPaint)
    }
}
