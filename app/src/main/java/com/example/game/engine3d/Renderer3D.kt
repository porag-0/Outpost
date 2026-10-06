package com.example.game.engine3d

import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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

        // 1. Draw Advanced Space / Skybox Background with Stars, Earth, Sun & Lens Flare
        drawCelestialAtmosphere(drawScope, width, height, cameraPos, cameraTarget, isMars, emergencyAlertActive, emergencyPulse, showLensFlares)

        // 2. Setup Camera View and Perspective Projection Matrices
        val viewMatrix = Matrix4.lookAt(cameraPos, cameraTarget, Vector3.UP)
        // Widescreen FOV from settings
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

                // If completely behind camera near plane
                if (centerCam.z > meshRadius + 0.2f) continue
                val zDist = -centerCam.z
                // If beyond maximum view distance
                if (zDist > 88f + meshRadius) continue
                // Horizontal and vertical frustum culling
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
                    poly.averageDepth = avgZ
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
                    poly.averageDepth = avgZ
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

        // 6. Draw Polygons with clean aerospace edge panel accents & procedural textures
        for (poly in activePolygons) {
            polygonPath.reset()
            polygonPath.moveTo(poly.xCoords[0], poly.yCoords[0])
            for (i in 1 until poly.count) {
                polygonPath.lineTo(poly.xCoords[i], poly.yCoords[i])
            }
            polygonPath.close()

            fillPaint.color = poly.finalColor
            canvas.drawPath(polygonPath, fillPaint)

            // Procedural In-Game Textures (LOD: skip micro-details beyond 30m)
            val isClose = poly.averageDepth > -30f
            when (poly.texture) {
                SurfaceTexture.SOLAR_CELLS -> {
                    strokePaint.color = 0xAA80DEEA.toInt()
                    strokePaint.strokeWidth = 1.2f
                    canvas.drawPath(polygonPath, strokePaint)
                    if (isClose && poly.count >= 4) {
                        val xTop = poly.xCoords[0] + (poly.xCoords[1] - poly.xCoords[0]) * 0.5f
                        val yTop = poly.yCoords[0] + (poly.yCoords[1] - poly.yCoords[0]) * 0.5f
                        val xBot = poly.xCoords[3] + (poly.xCoords[2] - poly.xCoords[3]) * 0.5f
                        val yBot = poly.yCoords[3] + (poly.yCoords[2] - poly.yCoords[3]) * 0.5f
                        strokePaint.color = 0xCCFFFFFF.toInt()
                        strokePaint.strokeWidth = 1.4f
                        canvas.drawLine(xTop, yTop, xBot, yBot, strokePaint)
                    }
                }
                SurfaceTexture.PAVER_TILES -> {
                    if (isClose) {
                        strokePaint.color = 0x44000000.toInt()
                        strokePaint.strokeWidth = 1.3f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }
                SurfaceTexture.REGOLITH_DUST -> {
                    if (wireframeHighlight && poly.averageDepth > -22f) {
                        strokePaint.color = ((poly.finalColor and 0x00FFFFFF) or 0x1A000000)
                        strokePaint.strokeWidth = 0.8f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }
                SurfaceTexture.CRATER_ROCK -> {
                    if (isClose) {
                        strokePaint.color = 0x44000000.toInt()
                        strokePaint.strokeWidth = 1.2f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }
                SurfaceTexture.METAL_PANELS -> {
                    if (isClose) {
                        strokePaint.color = 0x55FFFFFF.toInt()
                        strokePaint.strokeWidth = 1.1f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }
                SurfaceTexture.THERMAL_GOLD_FOIL -> {
                    if (isClose) {
                        strokePaint.color = 0x55FFD700.toInt()
                        strokePaint.strokeWidth = 1.0f
                        canvas.drawPath(polygonPath, strokePaint)
                    }
                }
                SurfaceTexture.GREENHOUSE_GLASS -> {
                    strokePaint.color = 0x88E0F7FA.toInt()
                    strokePaint.strokeWidth = 1.2f
                    canvas.drawPath(polygonPath, strokePaint)
                }
                SurfaceTexture.HAZARD_STRIPES -> {
                    strokePaint.color = 0xFF212121.toInt()
                    strokePaint.strokeWidth = 1.6f
                    canvas.drawPath(polygonPath, strokePaint)
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
            // Draw contact shadow discs for entities above ground
            if (mesh.name == "Astronaut" || mesh.name == "PlanetaryRover" || mesh.name == "SpaceLander" ||
                mesh.name == "BeaconPylon" || mesh.name == "CryoStorage" || mesh.name == "ScienceRack" ||
                mesh.name == "ScoutDrone"
            ) {
                // Soft directional shadow offset cast by celestial sun
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

        // Lambertian directional lighting
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

        // Headlight spotlight beam
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

            // Gold/Cyan visor specular boost
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

        if (isMars) {
            // Martian butterscotch/dusty orange sky
            val topColor = if (emergencyAlertActive) 0xFF3E0A03.toInt() else 0xFF23100A.toInt()
            val horizColor = if (emergencyAlertActive) 0xFF7A1C0B.toInt() else 0xFF6D341D.toInt()

            fillPaint.shader = LinearGradient(0f, 0f, 0f, horizonY.coerceAtLeast(10f), topColor, horizColor, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width, height, fillPaint)
            fillPaint.shader = null

            // Martian dust haze layer
            fillPaint.color = 0x558D4320.toInt()
            canvas.drawRect(0f, (horizonY - 70f).coerceAtLeast(0f), width, (horizonY + 70f).coerceAtMost(height), fillPaint)

            // Phobos (inner moon of Mars)
            val phobosX = width * 0.74f
            val phobosY = (height * 0.22f).coerceAtMost(horizonY - 30f)
            fillPaint.color = 0xFF8D6E63.toInt()
            canvas.drawCircle(phobosX, phobosY, 9f, fillPaint)
            fillPaint.color = 0xFF5D4037.toInt()
            canvas.drawCircle(phobosX + 2f, phobosY - 2f, 3.5f, fillPaint) // Stickney crater on Phobos

            // Deimos (outer tiny moon of Mars)
            val deimosX = width * 0.86f
            val deimosY = (height * 0.16f).coerceAtMost(horizonY - 45f)
            fillPaint.color = 0xFFA1887F.toInt()
            canvas.drawCircle(deimosX, deimosY, 4.5f, fillPaint)

            // Earth as Azure Morning Star
            val earthStarX = width * 0.62f
            val earthStarY = (height * 0.20f).coerceAtMost(horizonY - 40f)
            fillPaint.color = 0xFF80D8FF.toInt()
            canvas.drawCircle(earthStarX, earthStarY, 3.5f, fillPaint)
        } else {
            // Deep space void with Milky Way dust lane
            fillPaint.color = 0xFF03060C.toInt()
            canvas.drawRect(0f, 0f, width, height, fillPaint)

            // Cosmic Nebula Swirls (Cyan and Magenta Deep Space Gas Clouds)
            if (cachedNebulaPaint1 == null || cachedSkyWidth != width || cachedSkyHeight != height) {
                cachedSkyWidth = width
                cachedSkyHeight = height
                cachedNebulaPaint1 = Paint().apply {
                    isAntiAlias = true
                    shader = RadialGradient(width * 0.35f, height * 0.25f, width * 0.45f, 0x229C27B0.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
                }
                cachedNebulaPaint2 = Paint().apply {
                    isAntiAlias = true
                    shader = RadialGradient(width * 0.70f, height * 0.30f, width * 0.40f, 0x1800E5FF.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
                }
                cachedMwPaint = Paint().apply {
                    isAntiAlias = true
                    shader = LinearGradient(0f, 0f, width, (height * 0.5f).coerceAtLeast(100f), 0x22304FFE.toInt(), 0x1100E5FF.toInt(), Shader.TileMode.CLAMP)
                }
            }

            cachedNebulaPaint1?.let { canvas.drawRect(0f, 0f, width, horizonY.coerceAtLeast(60f), it) }
            cachedNebulaPaint2?.let { canvas.drawRect(0f, 0f, width, horizonY.coerceAtLeast(60f), it) }
            cachedMwPaint?.let { canvas.drawRect(0f, 0f, width, horizonY.coerceAtLeast(50f), it) }

            // Stars with varied magnitudes and twinkling
            val starCount = 95
            for (i in 0 until starCount) {
                val sx = (sin(i * 117.43f) * 0.5f + 0.5f) * width
                val sy = (cos(i * 73.19f) * 0.5f + 0.5f) * horizonY.coerceAtLeast(80f)
                if (sy < horizonY) {
                    val bright = ((sin(i * 3.1f + System.currentTimeMillis() * 0.002f) * 0.3f + 0.7f) * 255).toInt()
                    starPaint.color = (bright shl 24) or 0x00FFFFFF
                    val starSize = if (i % 7 == 0) 2.2f else 1.2f
                    canvas.drawCircle(sx, sy, starSize, starPaint)
                }
            }

            // Distant Jupiter with Cloud Bands
            val jupX = width * 0.14f
            val jupY = (height * 0.26f).coerceAtMost(horizonY - 35f)
            fillPaint.color = 0xFFD7CCC8.toInt()
            canvas.drawCircle(jupX, jupY, 11f, fillPaint)
            fillPaint.color = 0xFF8D6E63.toInt()
            canvas.drawRect(jupX - 10.5f, jupY - 3f, jupX + 10.5f, jupY - 0.5f, fillPaint)
            canvas.drawRect(jupX - 10.5f, jupY + 1.2f, jupX + 10.5f, jupY + 3.8f, fillPaint)
            fillPaint.color = 0xFFFF7043.toInt()
            canvas.drawCircle(jupX + 3f, jupY + 2.5f, 2.2f, fillPaint) // Great Red Spot

            // Distant High-Resolution Earth ("The Blue Marble") in lunar sky
            val earthX = width * 0.82f
            val earthY = (height * 0.24f).coerceAtMost(horizonY - 40f)

            // Earth outer atmospheric Rayleigh glow
            glowPaint.shader = RadialGradient(earthX, earthY, 46f, 0x6640C4FF.toInt(), 0x00000000.toInt(), Shader.TileMode.CLAMP)
            canvas.drawCircle(earthX, earthY, 46f, glowPaint)
            glowPaint.shader = null

            // Earth Oceans
            fillPaint.color = 0xFF1565C0.toInt()
            canvas.drawCircle(earthX, earthY, 34f, fillPaint)

            // Continents
            fillPaint.color = 0xFF388E3C.toInt()
            canvas.drawCircle(earthX - 7f, earthY + 3f, 14f, fillPaint)
            canvas.drawCircle(earthX + 9f, earthY - 5f, 11f, fillPaint)

            // Swirling White Cloud Covers
            fillPaint.color = 0xDDFFFFFF.toInt()
            canvas.drawCircle(earthX + 11f, earthY - 9f, 10f, fillPaint)
            canvas.drawCircle(earthX - 5f, earthY + 11f, 9f, fillPaint)
            canvas.drawCircle(earthX + 2f, earthY + 2f, 7f, fillPaint)
        }

        // Distant Planetary Horizon Mountain Ridges & Crater Rims
        val yawAngle = kotlin.math.atan2(camDir.x, camDir.z)
        drawDistantPlanetaryHorizon(canvas, width, height, horizonY, yawAngle, isMars, emergencyAlertActive)

        // Sun & Dynamic Anamorphic Lens Flare
        val sunX = width * 0.24f
        val sunY = (height * 0.18f).coerceAtMost(horizonY - 30f)

        // Sun Corona Halo
        val coronaRadius = if (emergencyAlertActive) 75f else 60f
        val coronaColor = if (emergencyAlertActive) 0x66FF1744.toInt() else 0x55FFF59D.toInt()
        glowPaint.shader = RadialGradient(sunX, sunY, coronaRadius, coronaColor, 0x00000000.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(sunX, sunY, coronaRadius, glowPaint)
        glowPaint.shader = null

        // Bright Sun Disk
        fillPaint.color = 0xFFFFFEE0.toInt()
        canvas.drawCircle(sunX, sunY, 18f, fillPaint)

        // Lens Flare Anamorphic Glare Streak
        if (showLensFlares) {
            val flarePaint = Paint().apply {
                strokeWidth = 2.5f
                color = if (emergencyAlertActive) 0x66FF5252.toInt() else 0x5500E5FF.toInt()
            }
            canvas.drawLine(sunX - 160f, sunY, sunX + 160f, sunY, flarePaint)
            canvas.drawCircle(sunX * 1.3f, sunY * 1.25f, 14f, flarePaint)
            canvas.drawCircle(sunX * 1.6f, sunY * 1.55f, 8f, flarePaint)
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

        // Multi-frequency scanner radar circles
        val animPulse = (System.currentTimeMillis() % 1600L) / 1600f
        val pulseRadius1 = animPulse * 160f
        val pulseRadius2 = ((animPulse + 0.5f) % 1.0f) * 160f

        sweepPaint.alpha = ((1f - animPulse) * 220).toInt()
        canvas.drawCircle(cx, cy, pulseRadius1, sweepPaint)

        sweepPaint.alpha = (((1f - (pulseRadius2 / 160f))) * 220).toInt()
        canvas.drawCircle(cx, cy, pulseRadius2, sweepPaint)

        // Target reticle crosshairs
        sweepPaint.alpha = 180
        sweepPaint.strokeWidth = 1.5f
        canvas.drawLine(cx - 30f, cy, cx + 30f, cy, sweepPaint)
        canvas.drawLine(cx, cy - 30f, cx, cy + 30f, sweepPaint)
    }
}
