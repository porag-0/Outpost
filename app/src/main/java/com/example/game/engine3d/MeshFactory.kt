package com.example.game.engine3d

import kotlin.math.*

object MeshFactory {

    enum class AstronautPose {
        IDLE, WALK, RUN, JUMP, CROUCH, SCAN, REPAIR
    }

    // -------------------------------------------------------------
    // ANALYTICAL TERRAIN ELEVATION & SURFACE NORMALS
    // -------------------------------------------------------------
    fun getTerrainHeight(x: Float, z: Float): Float {
        val distCenter = hypot(x, z)
        // Smoothly flatten central outpost plaza within 18m with Hermite smoothstep
        val centerFlatten = if (distCenter < 18f) {
            val t = (distCenter / 18f).coerceIn(0f, 1f)
            t * t * (3f - 2f * t)
        } else 1.0f

        // Natural undulating regolith dunes and tectonic micro-ridges
        var h = (sin(x * 0.052f) * cos(z * 0.052f) * 1.85f +
                sin(x * 0.11f + z * 0.08f) * 0.42f +
                cos(x * 0.035f - z * 0.045f) * 0.55f)

        // Crater 1: South-East exploration crater (center at 24f, 26f) with raised rim & central peak
        val d1 = hypot(x - 24f, z - 26f)
        if (d1 < 17f) {
            if (d1 < 8.5f) {
                val depth = (1f - d1 / 8.5f)
                h -= depth * depth * 3.4f
                // Rebound central uplift peak in center of impact crater
                if (d1 < 2.5f) {
                    val peak = (1f - d1 / 2.5f)
                    h += peak * peak * 0.95f
                }
            } else {
                val rim = sin((d1 - 8.5f) / 8.5f * PI.toFloat())
                h += rim * 1.45f
            }
        }

        // Crater 2: North-West crater (center at -30f, -18f)
        val d2 = hypot(x + 30f, z + 18f)
        if (d2 < 19f) {
            if (d2 < 9.8f) {
                val depth = (1f - d2 / 9.8f)
                h -= depth * depth * 3.8f
            } else {
                val rim = sin((d2 - 9.8f) / 9.2f * PI.toFloat())
                h += rim * 1.5f
            }
        }

        // Crater 3: East Ridge Crater (center at 32f, -12f)
        val d3 = hypot(x - 32f, z + 12f)
        if (d3 < 14f) {
            if (d3 < 7.0f) {
                val depth = (1f - d3 / 7.0f)
                h -= depth * depth * 2.5f
            } else {
                val rim = sin((d3 - 7.0f) / 7.0f * PI.toFloat())
                h += rim * 1.2f
            }
        }

        // Landing pad area: flat at x = -24f, z = 4f
        val distLander = hypot(x - (-24f), z - 4f)
        if (distLander < 9.5f) {
            val padBlend = ((distLander - 5.8f) / 3.7f).coerceIn(0f, 1f)
            h *= (padBlend * padBlend * (3f - 2f * padBlend))
        }

        return h * centerFlatten
    }

    fun getTerrainNormal(x: Float, z: Float): Vector3 {
        val eps = 0.35f
        val hL = getTerrainHeight(x - eps, z)
        val hR = getTerrainHeight(x + eps, z)
        val hD = getTerrainHeight(x, z - eps)
        val hU = getTerrainHeight(x, z + eps)
        val dx = (hR - hL) / (2f * eps)
        val dz = (hU - hD) / (2f * eps)
        return Vector3(-dx, 1f, -dz).normalized()
    }

    // -------------------------------------------------------------
    // UNIVERSAL SOLID BOX BUILDER WITH ACCURATE OUTWARD NORMALS
    // -------------------------------------------------------------
    fun addBoxToMesh(
        vertices: MutableList<Vector3>,
        faces: MutableList<Face3D>,
        center: Vector3,
        size: Vector3,
        color: Long,
        rotX: Float = 0f,
        rotY: Float = 0f,
        rotZ: Float = 0f,
        emissive: Boolean = false,
        specular: Float = 0.25f,
        texture: SurfaceTexture = SurfaceTexture.NONE
    ) {
        val hx = size.x / 2f
        val hy = size.y / 2f
        val hz = size.z / 2f
        val b = vertices.size

        val localVerts = arrayOf(
            Vector3(-hx, -hy, -hz), Vector3(hx, -hy, -hz),
            Vector3(hx, hy, -hz), Vector3(-hx, hy, -hz),
            Vector3(-hx, -hy, hz), Vector3(hx, -hy, hz),
            Vector3(hx, hy, hz), Vector3(-hx, hy, hz)
        )
        for (lv in localVerts) {
            var v = lv
            if (rotX != 0f) v = v.rotateX(rotX)
            if (rotY != 0f) v = v.rotateY(rotY)
            if (rotZ != 0f) v = v.rotateZ(rotZ)
            vertices.add(center + v)
        }

        // All 6 cube faces defined with correct outward normals and doubleSided = true (NO MISSING BLOCKS/ROOFS)
        faces.add(Face3D(b + 4, b + 5, b + 6, b + 7, color, emissive, doubleSided = true, specular = specular, texture = texture)) // Front (+Z)
        faces.add(Face3D(b + 1, b + 0, b + 3, b + 2, color, emissive, doubleSided = true, specular = specular, texture = texture)) // Back (-Z)
        faces.add(Face3D(b + 7, b + 6, b + 2, b + 3, color, emissive, doubleSided = true, specular = specular, texture = texture)) // Top (+Y)
        faces.add(Face3D(b + 0, b + 1, b + 5, b + 4, color, emissive, doubleSided = true, specular = specular, texture = texture)) // Bottom (-Y)
        faces.add(Face3D(b + 5, b + 1, b + 2, b + 6, color, emissive, doubleSided = true, specular = specular, texture = texture)) // Right (+X)
        faces.add(Face3D(b + 0, b + 4, b + 7, b + 3, color, emissive, doubleSided = true, specular = specular, texture = texture)) // Left (-X)
    }

    // -------------------------------------------------------------
    // HIGH-FIDELITY RIGGED ASTRONAUT EVA SUIT WITH SMOOTH LOW-G KINEMATICS
    // -------------------------------------------------------------
    fun createAstronautMesh(
        suitColor: Long = 0xFFECEFF1,       // Classic Extravehicular White
        visorColor: Long = 0xFFFFD700,      // Reflective Gold Foil
        accentColor: Long = 0xFF0288D1,     // Orbital Cyan / Blue
        backpackColor: Long = 0xFF37474F,   // Life support PLSS dark grey
        pose: AstronautPose = AstronautPose.IDLE,
        animTime: Float = 0f,
        landingSquat: Float = 0f,
        bankAngle: Float = 0f,              // Dynamic centripetal lean into turns
        strideSpeed: Float = 1.0f,          // Dynamic stride velocity tracking
        slopePitch: Float = 0f              // Adapts body posture to terrain slope
    ): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            rotX: Float = 0f,
            rotY: Float = 0f,
            rotZ: Float = 0f,
            emissive: Boolean = false,
            specular: Float = 0.25f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive, specular, texture)
        }

        // Kinematic locomotion parameters with joint-angle constraints
        var leftHipAngle = 0f
        var rightHipAngle = 0f
        var leftKneeBend = 0f
        var rightKneeBend = 0f
        var leftAnkleAngle = 0f
        var rightAnkleAngle = 0f
        var leftArmAngle = 0f
        var rightArmAngle = 0f
        var leftForearmAngle = 0f
        var rightForearmAngle = 0f

        var pelvisYaw = 0f
        var torsoBob = 0f
        var torsoRoll = bankAngle
        var torsoPitch = slopePitch * 0.45f
        var torsoYaw = 0f
        var hipSway = 0f
        var crouchY = -landingSquat * 0.22f

        when (pose) {
            AstronautPose.IDLE -> {
                val breath = sin(animTime * 1.8f)
                torsoBob = breath * 0.016f
                torsoPitch += sin(animTime * 1.1f) * 0.012f
                hipSway = sin(animTime * 0.85f) * 0.018f
                torsoRoll += sin(animTime * 0.85f) * 0.015f

                leftArmAngle = 0.10f + breath * 0.02f
                rightArmAngle = 0.08f + breath * 0.02f
                leftForearmAngle = 0.16f
                rightForearmAngle = 0.18f

                leftHipAngle = 0.04f
                rightHipAngle = -0.04f
                leftKneeBend = 0.06f
                rightKneeBend = 0.06f
            }
            AstronautPose.WALK -> {
                val phase = animTime * (5.2f * strideSpeed.coerceIn(0.5f, 1.8f))
                val sinP = sin(phase)
                val cosP = cos(phase)

                pelvisYaw = sinP * 0.12f
                torsoYaw = -pelvisYaw * 0.65f

                leftHipAngle = sinP * 0.46f
                rightHipAngle = -sinP * 0.46f

                leftKneeBend = (-sinP).coerceAtLeast(0f) * 0.62f + landingSquat * 0.35f
                rightKneeBend = (sinP).coerceAtLeast(0f) * 0.62f + landingSquat * 0.35f

                leftAnkleAngle = (-cosP * 0.22f).coerceIn(-0.25f, 0.35f)
                rightAnkleAngle = (cosP * 0.22f).coerceIn(-0.25f, 0.35f)

                leftArmAngle = -sinP * 0.40f
                rightArmAngle = sinP * 0.35f
                leftForearmAngle = 0.22f + abs(sinP) * 0.18f
                rightForearmAngle = 0.20f + abs(sinP) * 0.15f

                torsoBob = abs(sinP) * 0.07f
                torsoPitch += -0.07f
                torsoRoll += -sinP * 0.03f
                hipSway = sinP * 0.04f
            }
            AstronautPose.RUN -> {
                val phase = animTime * (8.5f * strideSpeed.coerceIn(0.8f, 1.6f))
                val sinP = sin(phase)
                val cosP = cos(phase)

                pelvisYaw = sinP * 0.18f
                torsoYaw = -pelvisYaw * 0.70f

                leftHipAngle = sinP * 0.75f
                rightHipAngle = -sinP * 0.75f

                leftKneeBend = (-sinP).coerceAtLeast(0f) * 0.95f + landingSquat * 0.45f
                rightKneeBend = (sinP).coerceAtLeast(0f) * 0.95f + landingSquat * 0.45f

                leftAnkleAngle = (-cosP * 0.35f).coerceIn(-0.35f, 0.45f)
                rightAnkleAngle = (cosP * 0.35f).coerceIn(-0.35f, 0.45f)

                leftArmAngle = -sinP * 0.70f
                rightArmAngle = sinP * 0.65f
                leftForearmAngle = 0.42f + abs(sinP) * 0.28f
                rightForearmAngle = 0.38f + abs(sinP) * 0.25f

                torsoBob = abs(sinP) * 0.11f
                torsoPitch += -0.18f
                torsoRoll += -sinP * 0.05f
                hipSway = sinP * 0.055f
            }
            AstronautPose.JUMP -> {
                leftHipAngle = 0.36f
                rightHipAngle = 0.26f
                leftKneeBend = 0.72f
                rightKneeBend = 0.58f
                leftAnkleAngle = 0.30f
                rightAnkleAngle = 0.30f
                leftArmAngle = -0.50f
                rightArmAngle = -0.45f
                leftForearmAngle = 0.40f
                rightForearmAngle = 0.38f
                torsoPitch += 0.10f
                torsoBob = 0.14f
            }
            AstronautPose.CROUCH -> {
                crouchY -= 0.38f
                leftHipAngle = 0.70f
                rightHipAngle = 0.70f
                leftKneeBend = 0.95f
                rightKneeBend = 0.95f
                leftAnkleAngle = 0.15f
                rightAnkleAngle = 0.15f
                torsoPitch += -0.25f
                leftForearmAngle = 0.45f
                rightForearmAngle = 0.45f
            }
            AstronautPose.SCAN -> {
                torsoBob = sin(animTime * 1.8f) * 0.015f
                rightArmAngle = -0.82f
                rightForearmAngle = 0.50f
                leftArmAngle = 0.18f
                leftForearmAngle = 0.28f
            }
            AstronautPose.REPAIR -> {
                crouchY -= 0.18f
                torsoPitch += -0.28f
                rightArmAngle = -0.88f + sin(animTime * 7.5f) * 0.15f
                rightForearmAngle = 0.65f
                leftArmAngle = -0.42f
                leftForearmAngle = 0.52f
            }
        }

        val yBase = torsoBob + crouchY

        // 1. Torso & Chest Pack with smooth pitch, roll and counter-yaw
        val torsoCenter = Vector3(hipSway, 1.15f + yBase, 0f)
        addBox(
            center = torsoCenter,
            size = Vector3(0.54f, 0.68f, 0.36f),
            color = suitColor,
            rotX = torsoPitch,
            rotY = torsoYaw,
            rotZ = torsoRoll,
            specular = 0.35f,
            texture = SurfaceTexture.METAL_PANELS
        )

        // CDU (Chest Display Unit) with telemetry LEDs
        val cduOffset = Vector3(0f, 0.11f, 0.19f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll)
        addBox(
            center = torsoCenter + cduOffset,
            size = Vector3(0.26f, 0.22f, 0.04f),
            color = 0xFF1A237E,
            rotX = torsoPitch,
            rotY = torsoYaw,
            rotZ = torsoRoll,
            emissive = true,
            specular = 0.6f
        )
        addBox(torsoCenter + cduOffset + Vector3(-0.08f, 0.06f, 0.025f), Vector3(0.04f, 0.04f, 0.02f), 0xFF00E676, emissive = true)
        addBox(torsoCenter + cduOffset + Vector3(0f, 0.06f, 0.025f), Vector3(0.04f, 0.04f, 0.02f), 0xFF00E5FF, emissive = true)
        addBox(torsoCenter + cduOffset + Vector3(0.08f, 0.06f, 0.025f), Vector3(0.04f, 0.04f, 0.02f), accentColor, emissive = true)
        addBox(torsoCenter + cduOffset + Vector3(0f, -0.04f, 0.025f), Vector3(0.18f, 0.03f, 0.02f), 0xFF00E5FF, emissive = true)

        // Utility Chest Harness Straps
        addBox(torsoCenter + Vector3(-0.16f, 0f, 0.185f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.06f, 0.60f, 0.02f), 0xFF263238, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + Vector3(0.16f, 0f, 0.185f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.06f, 0.60f, 0.02f), 0xFF263238, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)

        // 2. PLSS Backpack (Portable Life Support System)
        val plssOffset = Vector3(0f, 0.07f, -0.28f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll)
        addBox(
            center = torsoCenter + plssOffset,
            size = Vector3(0.48f, 0.64f, 0.24f),
            color = backpackColor,
            rotX = torsoPitch,
            rotY = torsoYaw,
            rotZ = torsoRoll,
            specular = 0.45f
        )
        addBox(torsoCenter + plssOffset + Vector3(0f, 0.33f, 0f), Vector3(0.40f, 0.04f, 0.20f), 0xFFCFD8DC, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll, specular = 0.6f)
        addBox(torsoCenter + plssOffset + Vector3(-0.14f, 0.03f, -0.12f), Vector3(0.14f, 0.52f, 0.14f), 0xFFECEFF1, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll, specular = 0.5f)
        addBox(torsoCenter + plssOffset + Vector3(0.14f, 0.03f, -0.12f), Vector3(0.14f, 0.52f, 0.14f), 0xFFECEFF1, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll, specular = 0.5f)
        addBox(torsoCenter + plssOffset + Vector3(-0.14f, 0.31f, -0.12f), Vector3(0.08f, 0.08f, 0.08f), 0xFFFFB300, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + plssOffset + Vector3(0.14f, 0.31f, -0.12f), Vector3(0.08f, 0.08f, 0.08f), 0xFFFFB300, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + plssOffset + Vector3(-0.20f, 0.33f, 0f), Vector3(0.08f, 0.10f, 0.08f), 0xFF90A4AE, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + plssOffset + Vector3(0.20f, 0.33f, 0f), Vector3(0.08f, 0.10f, 0.08f), 0xFF90A4AE, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + plssOffset + Vector3(0.20f, 0.54f, -0.04f), Vector3(0.02f, 0.36f, 0.02f), 0xFFECEFF1, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + plssOffset + Vector3(0.20f, 0.72f, -0.04f), Vector3(0.05f, 0.05f, 0.05f), 0xFF00E676, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll, emissive = true)

        // 3. Helmet & Stabilized Gold Foil Visor Shield
        val headPitch = torsoPitch * 0.35f
        val headOffset = Vector3(0f, 0.53f, 0f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll)
        val helmetCenter = torsoCenter + headOffset
        addBox(helmetCenter, Vector3(0.44f, 0.42f, 0.42f), suitColor, rotX = headPitch, rotY = torsoYaw, rotZ = torsoRoll, specular = 0.45f)
        addBox(helmetCenter + Vector3(0f, 0f, 0.16f).rotateX(headPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.36f, 0.28f, 0.18f), visorColor, rotX = headPitch, rotY = torsoYaw, rotZ = torsoRoll, emissive = true, specular = 0.95f, texture = SurfaceTexture.THERMAL_GOLD_FOIL)
        addBox(helmetCenter + Vector3(0f, 0f, 0.12f).rotateX(headPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.40f, 0.32f, 0.04f), 0xFF263238, rotX = headPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(helmetCenter + Vector3(-0.21f, 0.12f, 0.14f).rotateX(headPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.06f, 0.06f, 0.09f), 0xFFFFF9C4, rotX = headPitch, rotY = torsoYaw, rotZ = torsoRoll, emissive = true)
        addBox(helmetCenter + Vector3(0.21f, 0.12f, 0.14f).rotateX(headPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.06f, 0.06f, 0.09f), 0xFFFFF9C4, rotX = headPitch, rotY = torsoYaw, rotZ = torsoRoll, emissive = true)

        // Shoulder Pauldron Armor
        addBox(torsoCenter + Vector3(-0.32f, 0.25f, 0f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.18f, 0.12f, 0.28f), accentColor, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll, specular = 0.5f)
        addBox(torsoCenter + Vector3(0.32f, 0.25f, 0f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.18f, 0.12f, 0.28f), accentColor, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll, specular = 0.5f)

        // Life-Support Umbilical Conduits
        addBox(torsoCenter + Vector3(-0.16f, -0.07f, -0.15f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.05f, 0.16f, 0.12f), 0xFF00E5FF, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)
        addBox(torsoCenter + Vector3(0.16f, -0.07f, -0.15f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll), Vector3(0.05f, 0.16f, 0.12f), 0xFFFF7043, rotX = torsoPitch, rotY = torsoYaw, rotZ = torsoRoll)

        // 4. Arms & Forearm Kinematics
        val leftShoulder = torsoCenter + Vector3(-0.36f, 0.20f, 0f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll)
        val leftUpperArmCenter = leftShoulder + Vector3(0f, -0.16f, 0f).rotateX(leftArmAngle).rotateZ(torsoRoll)
        addBox(leftUpperArmCenter, Vector3(0.18f, 0.32f, 0.20f), suitColor, rotX = leftArmAngle, rotZ = torsoRoll)
        addBox(leftUpperArmCenter + Vector3(0f, -0.16f, 0f).rotateX(leftArmAngle).rotateZ(torsoRoll), Vector3(0.15f, 0.06f, 0.17f), 0xFF455A64, rotX = leftArmAngle, rotZ = torsoRoll)

        val leftForearmCenter = leftUpperArmCenter + Vector3(0f, -0.22f, 0f).rotateX(leftArmAngle + leftForearmAngle).rotateZ(torsoRoll)
        addBox(leftForearmCenter, Vector3(0.16f, 0.28f, 0.18f), suitColor, rotX = leftArmAngle + leftForearmAngle, rotZ = torsoRoll)
        addBox(leftForearmCenter + Vector3(-0.02f, 0f, 0.09f).rotateX(leftArmAngle + leftForearmAngle).rotateZ(torsoRoll), Vector3(0.12f, 0.12f, 0.04f), 0xFF00E5FF, rotX = leftArmAngle + leftForearmAngle, rotZ = torsoRoll, emissive = true)
        val leftGloveCenter = leftForearmCenter + Vector3(0f, -0.16f, 0f).rotateX(leftArmAngle + leftForearmAngle).rotateZ(torsoRoll)
        addBox(leftGloveCenter, Vector3(0.16f, 0.16f, 0.18f), 0xFF263238, rotX = leftArmAngle + leftForearmAngle, rotZ = torsoRoll)

        val rightShoulder = torsoCenter + Vector3(0.36f, 0.20f, 0f).rotateX(torsoPitch).rotateY(torsoYaw).rotateZ(torsoRoll)
        val rightUpperArmCenter = rightShoulder + Vector3(0f, -0.16f, 0f).rotateX(rightArmAngle).rotateZ(torsoRoll)
        addBox(rightUpperArmCenter, Vector3(0.18f, 0.32f, 0.20f), suitColor, rotX = rightArmAngle, rotZ = torsoRoll)
        addBox(rightUpperArmCenter + Vector3(0f, -0.16f, 0f).rotateX(rightArmAngle).rotateZ(torsoRoll), Vector3(0.15f, 0.06f, 0.17f), 0xFF455A64, rotX = rightArmAngle, rotZ = torsoRoll)

        val rightForearmCenter = rightUpperArmCenter + Vector3(0f, -0.22f, 0f).rotateX(rightArmAngle + rightForearmAngle).rotateZ(torsoRoll)
        addBox(rightForearmCenter, Vector3(0.16f, 0.28f, 0.18f), suitColor, rotX = rightArmAngle + rightForearmAngle, rotZ = torsoRoll)
        val rightGloveCenter = rightForearmCenter + Vector3(0f, -0.16f, 0f).rotateX(rightArmAngle + rightForearmAngle).rotateZ(torsoRoll)
        addBox(rightGloveCenter, Vector3(0.16f, 0.16f, 0.18f), 0xFF263238, rotX = rightArmAngle + rightForearmAngle, rotZ = torsoRoll)

        // Geological Multi-Tool / Scanner in Right Hand
        val toolPos = rightGloveCenter + Vector3(0f, -0.05f, 0.14f).rotateX(rightArmAngle + rightForearmAngle).rotateZ(torsoRoll)
        addBox(toolPos, Vector3(0.12f, 0.14f, 0.30f), 0xFF212121, rotX = rightArmAngle + rightForearmAngle, rotZ = torsoRoll)
        addBox(toolPos + Vector3(0f, 0f, 0.16f).rotateX(rightArmAngle + rightForearmAngle).rotateZ(torsoRoll), Vector3(0.08f, 0.08f, 0.06f), 0xFF00E5FF, rotX = rightArmAngle + rightForearmAngle, rotZ = torsoRoll, emissive = true)
        addBox(toolPos + Vector3(0f, 0.06f, 0.10f).rotateX(rightArmAngle + rightForearmAngle).rotateZ(torsoRoll), Vector3(0.02f, 0.04f, 0.14f), 0xFF00E676, rotX = rightArmAngle + rightForearmAngle, rotZ = torsoRoll, emissive = true)

        // 5. Waist / Tether Ring with Pelvis Twist
        val waistCenter = torsoCenter + Vector3(0f, -0.35f, 0f).rotateX(torsoPitch).rotateY(pelvisYaw).rotateZ(torsoRoll)
        addBox(waistCenter, Vector3(0.48f, 0.16f, 0.34f), 0xFF263238, rotX = torsoPitch, rotY = pelvisYaw, rotZ = torsoRoll)
        addBox(waistCenter + Vector3(-0.26f, -0.02f, 0f), Vector3(0.08f, 0.18f, 0.18f), 0xFFFFB300)

        // 6. Articulated Legs
        val leftHip = Vector3(-0.17f + hipSway, 0.72f + yBase, 0f).rotateY(pelvisYaw)
        val leftThighCenter = leftHip + Vector3(0f, -0.22f, 0f).rotateX(leftHipAngle).rotateZ(torsoRoll)
        addBox(leftThighCenter, Vector3(0.22f, 0.38f, 0.24f), suitColor, rotX = leftHipAngle, rotZ = torsoRoll)

        val leftKneeAngle = leftHipAngle + leftKneeBend
        val leftShinCenter = leftThighCenter + Vector3(0f, -0.26f, 0f).rotateX(leftKneeAngle).rotateZ(torsoRoll)
        addBox(leftShinCenter, Vector3(0.20f, 0.36f, 0.22f), suitColor, rotX = leftKneeAngle, rotZ = torsoRoll)
        addBox(leftShinCenter + Vector3(0f, 0.10f, 0.12f).rotateX(leftKneeAngle).rotateZ(torsoRoll), Vector3(0.18f, 0.15f, 0.05f), 0xFF37474F, rotX = leftKneeAngle, rotZ = torsoRoll)

        val leftBootAngle = leftKneeAngle + leftAnkleAngle
        val leftBootCenter = leftShinCenter + Vector3(0f, -0.19f, 0.04f).rotateX(leftBootAngle).rotateZ(torsoRoll)
        addBox(leftBootCenter, Vector3(0.24f, 0.18f, 0.35f), 0xFF212121, rotX = leftBootAngle, rotZ = torsoRoll)
        addBox(leftBootCenter + Vector3(0f, -0.08f, 0f).rotateX(leftBootAngle).rotateZ(torsoRoll), Vector3(0.26f, 0.06f, 0.37f), 0xFF607D8B, rotX = leftBootAngle, rotZ = torsoRoll)

        val rightHip = Vector3(0.17f + hipSway, 0.72f + yBase, 0f).rotateY(pelvisYaw)
        val rightThighCenter = rightHip + Vector3(0f, -0.22f, 0f).rotateX(rightHipAngle).rotateZ(torsoRoll)
        addBox(rightThighCenter, Vector3(0.22f, 0.38f, 0.24f), suitColor, rotX = rightHipAngle, rotZ = torsoRoll)

        val rightKneeAngle = rightHipAngle + rightKneeBend
        val rightShinCenter = rightThighCenter + Vector3(0f, -0.26f, 0f).rotateX(rightKneeAngle).rotateZ(torsoRoll)
        addBox(rightShinCenter, Vector3(0.20f, 0.36f, 0.22f), suitColor, rotX = rightKneeAngle, rotZ = torsoRoll)
        addBox(rightShinCenter + Vector3(0f, 0.10f, 0.12f).rotateX(rightKneeAngle).rotateZ(torsoRoll), Vector3(0.18f, 0.15f, 0.05f), 0xFF37474F, rotX = rightKneeAngle, rotZ = torsoRoll)

        val rightBootAngle = rightKneeAngle + rightAnkleAngle
        val rightBootCenter = rightShinCenter + Vector3(0f, -0.19f, 0.04f).rotateX(rightBootAngle).rotateZ(torsoRoll)
        addBox(rightBootCenter, Vector3(0.24f, 0.18f, 0.35f), 0xFF212121, rotX = rightBootAngle, rotZ = torsoRoll)
        addBox(rightBootCenter + Vector3(0f, -0.08f, 0f).rotateX(rightBootAngle).rotateZ(torsoRoll), Vector3(0.26f, 0.06f, 0.37f), 0xFF607D8B, rotX = rightBootAngle, rotZ = torsoRoll)

        return Mesh3D(vertices, faces, name = "Astronaut")
    }

    // -------------------------------------------------------------
    // HIGH-FIDELITY 6-WHEEL PLANETARY ROVER WITH ROCKER-BOGIE MOBILITY
    // -------------------------------------------------------------
    fun createRoverMesh(
        steerAngle: Float = 0f,
        wheelSpin: Float = 0f,
        pitchAngle: Float = 0f,
        rollAngle: Float = 0f
    ): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            rotX: Float = 0f,
            rotY: Float = 0f,
            rotZ: Float = 0f,
            emissive: Boolean = false,
            specular: Float = 0.35f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive, specular, texture)
        }

        // 1. Rover Central Chassis & Monocoque Hull with pitch and roll
        addBox(Vector3(0f, 0.90f, 0f), Vector3(1.70f, 0.75f, 2.90f), 0xFFECEFF1, rotX = pitchAngle, rotZ = rollAngle, specular = 0.4f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0f, 1.35f, -0.2f), Vector3(1.50f, 0.65f, 2.10f), 0xFF263238, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(0f, 1.30f, 0.85f), Vector3(1.40f, 0.50f, 0.55f), 0xFFFFB300, rotX = pitchAngle, rotZ = rollAngle, emissive = true, specular = 0.95f)

        // Front Tubular Impact Bullbar & Recovery Winch
        addBox(Vector3(0f, 0.65f, 1.55f), Vector3(1.60f, 0.12f, 0.12f), 0xFF37474F, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(0f, 0.45f, 1.55f), Vector3(0.35f, 0.30f, 0.25f), 0xFFFF5722, rotX = pitchAngle, rotZ = rollAngle)

        // 2. Headlight Floodlights
        addBox(Vector3(-0.65f, 0.90f, 1.48f), Vector3(0.28f, 0.22f, 0.12f), 0xFFFFFEE0, rotX = pitchAngle, rotZ = rollAngle, emissive = true)
        addBox(Vector3(0.65f, 0.90f, 1.48f), Vector3(0.28f, 0.22f, 0.12f), 0xFFFFFEE0, rotX = pitchAngle, rotZ = rollAngle, emissive = true)

        // 3. Rear Nuclear RTG Power Unit & Heat Radiators
        addBox(Vector3(0f, 1.05f, -1.60f), Vector3(0.90f, 0.55f, 0.60f), 0xFF1E1E1E, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(-0.48f, 1.10f, -1.60f), Vector3(0.08f, 0.65f, 0.65f), 0xFF455A64, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(0.48f, 1.10f, -1.60f), Vector3(0.08f, 0.65f, 0.65f), 0xFF455A64, rotX = pitchAngle, rotZ = rollAngle)

        // 4. Robotic Specimen Retrieval Arm on Front-Right
        addBox(Vector3(0.75f, 0.95f, 1.10f), Vector3(0.12f, 0.12f, 0.55f), 0xFF78909C, rotX = pitchAngle, rotY = 0.35f, rotZ = rollAngle)
        addBox(Vector3(0.92f, 0.90f, 1.45f), Vector3(0.14f, 0.14f, 0.14f), 0xFF00E5FF, rotX = pitchAngle, rotZ = rollAngle, emissive = true)

        // 5. Roof Dish & Mast Camera
        addBox(Vector3(-0.45f, 1.85f, -0.6f), Vector3(0.08f, 0.60f, 0.08f), 0xFFB0BEC5, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(-0.45f, 2.15f, -0.6f), Vector3(0.45f, 0.08f, 0.45f), 0xFFECEFF1, rotX = pitchAngle + 0.35f, rotZ = rollAngle)
        addBox(Vector3(0f, 1.95f, 0.2f), Vector3(0.14f, 0.28f, 0.14f), 0xFF212121, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(0f, 2.05f, 0.28f), Vector3(0.10f, 0.10f, 0.08f), 0xFF00E676, rotX = pitchAngle, rotZ = rollAngle, emissive = true)

        // 6. Rocker-Bogie 6-Wheel Articulated Suspension Arms
        addBox(Vector3(-0.95f, 0.60f, 0.50f), Vector3(0.12f, 0.12f, 1.40f), 0xFF37474F, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(0.95f, 0.60f, 0.50f), Vector3(0.12f, 0.12f, 1.40f), 0xFF37474F, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(-0.95f, 0.60f, -0.50f), Vector3(0.12f, 0.12f, 1.30f), 0xFF37474F, rotX = pitchAngle, rotZ = rollAngle)
        addBox(Vector3(0.95f, 0.60f, -0.50f), Vector3(0.12f, 0.12f, 1.30f), 0xFF37474F, rotX = pitchAngle, rotZ = rollAngle)

        val wheelPositions = listOf(
            Vector3(-1.12f, 0.45f, 1.05f),
            Vector3(1.12f, 0.45f, 1.05f),
            Vector3(-1.12f, 0.45f, 0.0f),
            Vector3(1.12f, 0.45f, 0.0f),
            Vector3(-1.12f, 0.45f, -1.05f),
            Vector3(1.12f, 0.45f, -1.05f)
        )

        for ((idx, pos) in wheelPositions.withIndex()) {
            val rotY = when (idx) {
                0, 1 -> steerAngle
                4, 5 -> -steerAngle * 0.5f
                else -> 0f
            }

            addBox(pos, Vector3(0.36f, 0.72f, 0.72f), 0xFF1C1C1C, rotX = wheelSpin, rotY = rotY)
            val hubOffset = if (pos.x < 0) -0.19f else 0.19f
            addBox(pos + Vector3(hubOffset, 0f, 0f), Vector3(0.06f, 0.36f, 0.36f), 0xFF90A4AE, rotY = rotY)
            val strutDir = if (pos.x < 0) 0.18f else -0.18f
            addBox(pos + Vector3(strutDir, 0.25f, 0f), Vector3(0.12f, 0.40f, 0.12f), 0xFF455A64)
        }

        return Mesh3D(vertices, faces, name = "PlanetaryRover")
    }

    // -------------------------------------------------------------
    // OPTIMIZED SMOOTH PLANETARY TERRAIN MESH WITH FULL ROOF/CEILING INTEGRITY
    // -------------------------------------------------------------
    fun createTerrain(isMars: Boolean = false, gridSize: Int = 18, spacing: Float = 6.2f): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val half = (gridSize * spacing) / 2f
        val vertexGrid = Array(gridSize + 1) { IntArray(gridSize + 1) }

        val baseGroundColor = if (isMars) 0xFFBF360C else 0xFF424242
        val craterFloorColor = if (isMars) 0xFF5D1D06 else 0xFF1E1E1E
        val highlandColor = if (isMars) 0xFFD84315 else 0xFF5E5E5E
        val regolithDustTint = if (isMars) 0xFFE64A19 else 0xFF4E4E4E

        for (iz in 0..gridSize) {
            val z = -half + iz * spacing
            for (ix in 0..gridSize) {
                val x = -half + ix * spacing
                val y = getTerrainHeight(x, z)
                vertexGrid[iz][ix] = vertices.size
                vertices.add(Vector3(x, y, z))
            }
        }

        for (iz in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                val v0 = vertexGrid[iz][ix]
                val v1 = vertexGrid[iz][ix + 1]
                val v2 = vertexGrid[iz + 1][ix + 1]
                val v3 = vertexGrid[iz + 1][ix]

                val avgX = (vertices[v0].x + vertices[v1].x + vertices[v2].x + vertices[v3].x) / 4f
                val avgY = (vertices[v0].y + vertices[v1].y + vertices[v2].y + vertices[v3].y) / 4f
                val avgZ = (vertices[v0].z + vertices[v1].z + vertices[v2].z + vertices[v3].z) / 4f

                val distLander = hypot(avgX - (-24f), avgZ - 4f)
                val distCenter = hypot(avgX, avgZ)

                val onPathToLander = (avgZ in 2f..6f && avgX in -24f..0f)
                val onPathToCrater = (abs(avgX - avgZ) < 3.2f && avgX in 0f..22f)

                val (color, texture) = when {
                    distLander < 8.2f -> {
                        when {
                            distLander < 2.5f -> Pair(0xFF212121, SurfaceTexture.PAVER_TILES)
                            distLander < 5.2f -> Pair(0xFF37474F, SurfaceTexture.PAVER_TILES)
                            distLander < 6.4f -> Pair(0xFFFFB300, SurfaceTexture.PAVER_TILES)
                            else -> Pair(0xFF263238, SurfaceTexture.PAVER_TILES)
                        }
                    }
                    distCenter < 7.2f -> {
                        Pair(if (distCenter < 3.6f) 0xFF455A64 else 0xFF37474F, SurfaceTexture.PAVER_TILES)
                    }
                    onPathToLander || onPathToCrater -> {
                        Pair(if (isMars) 0xFF8D2A08 else 0xFF333333, SurfaceTexture.REGOLITH_DUST)
                    }
                    avgY < -1.1f -> Pair(craterFloorColor, SurfaceTexture.REGOLITH_DUST)
                    avgY > 0.85f -> Pair(highlandColor, SurfaceTexture.REGOLITH_DUST)
                    (ix + iz) % 3 == 0 -> Pair(regolithDustTint, SurfaceTexture.REGOLITH_DUST)
                    else -> Pair(baseGroundColor, SurfaceTexture.REGOLITH_DUST)
                }

                val specular = if (distLander < 8.2f) 0.35f else 0.08f
                // Normal points UPWARDS (+Y) with (v0, v3, v2, v1) and doubleSided = true (NO MISSING TERRAIN QUADS)
                faces.add(Face3D(v0, v3, v2, v1, color, doubleSided = true, specular = specular, texture = texture))
            }
        }

        return Mesh3D(vertices, faces, name = "Terrain")
    }

    // -------------------------------------------------------------
    // PLANETARY BOULDER / CRAGGY ROCK FORMATION
    // -------------------------------------------------------------
    fun createBoulder(size: Vector3, isMars: Boolean = false): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val baseColor = if (isMars) 0xFF6B1D05 else 0xFF242526
        val midColor = if (isMars) 0xFF8D2A08 else 0xFF36393C
        val highlightColor = if (isMars) 0xFFA73A0C else 0xFF4A4E53
        val crestColor = if (isMars) 0xFFBD4A18 else 0xFF626870

        val hx = size.x / 2f
        val hy = size.y
        val hz = size.z / 2f

        // 1. Primary Craggy Boulder Polyhedron (Tiered multi-faceted geological geometry)
        val vBase = vertices.size
        val ring0 = arrayOf(
            Vector3(-hx * 0.90f, 0.02f, -hz * 0.85f),
            Vector3(0f, 0.01f, -hz * 0.95f),
            Vector3(hx * 0.85f, 0.03f, -hz * 0.80f),
            Vector3(hx * 0.95f, 0.02f, 0f),
            Vector3(hx * 0.90f, 0.01f, hz * 0.85f),
            Vector3(0f, 0.03f, hz * 0.92f),
            Vector3(-hx * 0.85f, 0.02f, hz * 0.88f),
            Vector3(-hx * 0.95f, 0.01f, 0f)
        )
        for (v in ring0) vertices.add(v)

        val ring1 = arrayOf(
            Vector3(-hx * 0.82f, hy * 0.38f, -hz * 0.75f),
            Vector3(0.08f * hx, hy * 0.42f, -hz * 0.88f),
            Vector3(hx * 0.78f, hy * 0.36f, -hz * 0.72f),
            Vector3(hx * 0.88f, hy * 0.45f, 0.12f * hz),
            Vector3(hx * 0.75f, hy * 0.39f, hz * 0.76f),
            Vector3(-0.05f * hx, hy * 0.44f, hz * 0.82f),
            Vector3(-hx * 0.76f, hy * 0.37f, hz * 0.74f),
            Vector3(-hx * 0.86f, hy * 0.41f, -0.08f * hz)
        )
        for (v in ring1) vertices.add(v)

        val ring2 = arrayOf(
            Vector3(-hx * 0.60f, hy * 0.75f, -hz * 0.55f),
            Vector3(hx * 0.55f, hy * 0.78f, -hz * 0.58f),
            Vector3(hx * 0.62f, hy * 0.74f, hz * 0.50f),
            Vector3(0f, hy * 0.82f, hz * 0.62f),
            Vector3(-hx * 0.58f, hy * 0.76f, hz * 0.54f),
            Vector3(-hx * 0.66f, hy * 0.73f, 0f)
        )
        for (v in ring2) vertices.add(v)

        val crestVerts = arrayOf(
            Vector3(-hx * 0.25f, hy * 0.98f, -hz * 0.22f),
            Vector3(hx * 0.30f, hy * 1.02f, -hz * 0.15f),
            Vector3(hx * 0.20f, hy * 0.95f, hz * 0.25f),
            Vector3(-hx * 0.22f, hy * 0.96f, hz * 0.20f)
        )
        for (v in crestVerts) vertices.add(v)

        // Lower-to-mid quads
        for (i in 0 until 8) {
            val next = (i + 1) % 8
            val b0 = vBase + i
            val b1 = vBase + next
            val m1 = vBase + 8 + next
            val m0 = vBase + 8 + i
            val col = if (i % 2 == 0) baseColor else midColor
            faces.add(Face3D(b0, b1, m1, m0, col, doubleSided = true, specular = 0.12f, texture = SurfaceTexture.CRATER_ROCK))
        }

        // Mid-to-shoulder faces
        val shoulderMap = arrayOf(
            Pair(0, 0), Pair(1, 1), Pair(2, 1), Pair(3, 2),
            Pair(4, 2), Pair(5, 3), Pair(6, 4), Pair(7, 5)
        )
        for (i in 0 until 8) {
            val next = (i + 1) % 8
            val m0 = vBase + 8 + i
            val m1 = vBase + 8 + next
            val s0 = vBase + 16 + shoulderMap[i].second
            val s1 = vBase + 16 + shoulderMap[next].second
            val col = if (i % 2 == 0) midColor else highlightColor
            if (s0 == s1) {
                faces.add(Face3D(m0, m1, s0, baseColor = col, doubleSided = true, specular = 0.16f, texture = SurfaceTexture.CRATER_ROCK))
            } else {
                faces.add(Face3D(m0, m1, s1, s0, col, doubleSided = true, specular = 0.18f, texture = SurfaceTexture.CRATER_ROCK))
            }
        }

        // Shoulder-to-crest faces
        for (i in 0 until 4) {
            val next = (i + 1) % 4
            val s0 = vBase + 16 + (i * 6 / 4)
            val s1 = vBase + 16 + (next * 6 / 4)
            val c1 = vBase + 22 + next
            val c0 = vBase + 22 + i
            faces.add(Face3D(s0, s1, c1, c0, highlightColor, doubleSided = true, specular = 0.22f, texture = SurfaceTexture.CRATER_ROCK))
        }

        // Top crest cap
        faces.add(Face3D(vBase + 22, vBase + 23, vBase + 24, vBase + 25, crestColor, doubleSided = true, specular = 0.25f, texture = SurfaceTexture.CRATER_ROCK))

        // 2. Secondary Companion Scree Rocks (Spall fragments at base for natural realism)
        fun addScree(center: Vector3, rad: Float) {
            val sb = vertices.size
            val s0 = center + Vector3(-rad * 0.7f, 0f, -rad * 0.6f)
            val s1 = center + Vector3(rad * 0.8f, 0f, -rad * 0.5f)
            val s2 = center + Vector3(rad * 0.6f, 0f, rad * 0.7f)
            val s3 = center + Vector3(-rad * 0.6f, 0f, rad * 0.8f)
            val sTop = center + Vector3(rad * 0.1f, rad * 0.65f, rad * 0.1f)
            vertices.add(s0); vertices.add(s1); vertices.add(s2); vertices.add(s3); vertices.add(sTop)
            faces.add(Face3D(sb + 0, sb + 1, sb + 4, baseColor = midColor, doubleSided = true, specular = 0.15f, texture = SurfaceTexture.CRATER_ROCK))
            faces.add(Face3D(sb + 1, sb + 2, sb + 4, baseColor = highlightColor, doubleSided = true, specular = 0.18f, texture = SurfaceTexture.CRATER_ROCK))
            faces.add(Face3D(sb + 2, sb + 3, sb + 4, baseColor = midColor, doubleSided = true, specular = 0.15f, texture = SurfaceTexture.CRATER_ROCK))
            faces.add(Face3D(sb + 3, sb + 0, sb + 4, baseColor = highlightColor, doubleSided = true, specular = 0.18f, texture = SurfaceTexture.CRATER_ROCK))
        }

        addScree(Vector3(hx * 0.85f, 0f, hz * 0.80f), size.x * 0.28f)
        addScree(Vector3(-hx * 0.80f, 0f, -hz * 0.75f), size.x * 0.22f)

        return Mesh3D(vertices, faces, name = "Boulder")
    }

    // -------------------------------------------------------------
    // HIGH-FIDELITY GEODESIC HABITAT COMMAND DOME
    // -------------------------------------------------------------
    fun createOutpostDome(radius: Float = 5.4f, height: Float = 4.2f): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            rotX: Float = 0f,
            rotY: Float = 0f,
            rotZ: Float = 0f,
            emissive: Boolean = false,
            specular: Float = 0.35f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive, specular, texture)
        }

        // 1. Reinforced Regolith Concrete Foundation Skirt
        val skirtSegs = 14
        for (i in 0 until skirtSegs) {
            val th0 = (i.toFloat() / skirtSegs) * (2f * PI.toFloat())
            val th1 = ((i + 1).toFloat() / skirtSegs) * (2f * PI.toFloat())
            val rIn = radius - 0.1f
            val rOut = radius + 0.45f

            val v0 = Vector3(rIn * cos(th0), 0.0f, rIn * sin(th0))
            val v1 = Vector3(rOut * cos(th0), 0.0f, rOut * sin(th0))
            val v2 = Vector3(rOut * cos(th1), 0.0f, rOut * sin(th1))
            val v3 = Vector3(rIn * cos(th1), 0.0f, rIn * sin(th1))

            val v0Top = Vector3(rIn * cos(th0), 0.35f, rIn * sin(th0))
            val v1Top = Vector3(rOut * cos(th0), 0.35f, rOut * sin(th0))
            val v2Top = Vector3(rOut * cos(th1), 0.35f, rOut * sin(th1))
            val v3Top = Vector3(rIn * cos(th1), 0.35f, rIn * sin(th1))

            val sb = vertices.size
            vertices.add(v0); vertices.add(v1); vertices.add(v2); vertices.add(v3)
            vertices.add(v0Top); vertices.add(v1Top); vertices.add(v2Top); vertices.add(v3Top)

            // Skirt outer face
            faces.add(Face3D(sb + 1, sb + 2, sb + 6, sb + 5, 0xFF37474F, doubleSided = true, specular = 0.3f, texture = SurfaceTexture.METAL_PANELS))
            // Skirt top face (gold thermal insulation seal)
            faces.add(Face3D(sb + 4, sb + 5, sb + 6, sb + 7, 0xFFFFD700, doubleSided = true, specular = 0.85f, texture = SurfaceTexture.THERMAL_GOLD_FOIL))
        }

        // 2. Dual-Frequency Geodesic Dome Shell with Composite Framing Ribs
        val rings = 5
        val segments = 14
        val grid = Array(rings + 1) { IntArray(segments) }

        for (r in 0..rings) {
            val phi = (r.toFloat() / rings) * (PI.toFloat() * 0.5f)
            val ringRadius = radius * cos(phi)
            val y = 0.35f + (height - 0.35f) * sin(phi)

            for (s in 0 until segments) {
                val theta = (s.toFloat() / segments) * (2f * PI.toFloat())
                val x = ringRadius * cos(theta)
                val z = ringRadius * sin(theta)
                grid[r][s] = vertices.size
                vertices.add(Vector3(x, y, z))
            }
        }

        for (r in 0 until rings) {
            for (s in 0 until segments) {
                val nextS = (s + 1) % segments
                val v0 = grid[r][s]
                val v1 = grid[r][nextS]
                val v2 = grid[r + 1][nextS]
                val v3 = grid[r + 1][s]

                val isCupola = (r == rings - 1)
                val isWindow = (r == 2 && s % 3 == 0)

                val color = when {
                    isCupola -> 0xFF80D8FF
                    isWindow -> 0xFF00B0FF
                    (r + s) % 2 == 0 -> 0xFFECEFF1
                    else -> 0xFFCFD8DC
                }

                val texture = when {
                    isCupola || isWindow -> SurfaceTexture.GREENHOUSE_GLASS
                    r == 0 -> SurfaceTexture.THERMAL_GOLD_FOIL
                    else -> SurfaceTexture.METAL_PANELS
                }

                faces.add(
                    Face3D(
                        v0, v1, v2, v3,
                        color,
                        emissive = isCupola || isWindow,
                        doubleSided = true,
                        specular = if (isCupola || isWindow) 0.95f else 0.45f,
                        texture = texture
                    )
                )
            }
        }

        // 3. Command Deck Observation Cupola Spire & Telemetry Beacon
        addBox(Vector3(0f, height + 0.35f, 0f), Vector3(0.12f, 0.70f, 0.12f), 0xFFB0BEC5, specular = 0.6f)
        addBox(Vector3(0f, height + 0.72f, 0f), Vector3(0.38f, 0.04f, 0.38f), 0xFF37474F)
        addBox(Vector3(0f, height + 0.85f, 0f), Vector3(0.18f, 0.22f, 0.18f), 0xFF00E676, emissive = true)

        // 4. Pressurized Outer Airlock Module with Safety Hazard Stripes (Z+)
        val w = 1.95f
        val h = 2.30f
        val z0 = radius - 0.2f
        val z1 = radius + 2.7f
        val bAirlock = vertices.size

        vertices.add(Vector3(-w / 2, 0f, z0))
        vertices.add(Vector3(w / 2, 0f, z0))
        vertices.add(Vector3(w / 2, h, z0))
        vertices.add(Vector3(-w / 2, h, z0))
        vertices.add(Vector3(-w / 2, 0f, z1))
        vertices.add(Vector3(w / 2, 0f, z1))
        vertices.add(Vector3(w / 2, h, z1))
        vertices.add(Vector3(-w / 2, h, z1))

        // Airlock Walls & Roof
        faces.add(Face3D(bAirlock + 7, bAirlock + 6, bAirlock + 2, bAirlock + 3, 0xFF607D8B, doubleSided = true, specular = 0.4f, texture = SurfaceTexture.METAL_PANELS)) // Roof
        faces.add(Face3D(bAirlock + 0, bAirlock + 4, bAirlock + 7, bAirlock + 3, 0xFF455A64, doubleSided = true, specular = 0.4f, texture = SurfaceTexture.METAL_PANELS)) // Left
        faces.add(Face3D(bAirlock + 5, bAirlock + 1, bAirlock + 2, bAirlock + 6, 0xFF455A64, doubleSided = true, specular = 0.4f, texture = SurfaceTexture.METAL_PANELS)) // Right

        // Airlock Outer Portal with Yellow-Black Hazard Chevron Border
        addBox(Vector3(0f, h + 0.10f, z1 + 0.04f), Vector3(w + 0.2f, 0.22f, 0.12f), 0xFFFFD600, texture = SurfaceTexture.HAZARD_STRIPES)
        addBox(Vector3(-w / 2 - 0.06f, h / 2, z1 + 0.04f), Vector3(0.14f, h, 0.12f), 0xFFFFD600, texture = SurfaceTexture.HAZARD_STRIPES)
        addBox(Vector3(w / 2 + 0.06f, h / 2, z1 + 0.04f), Vector3(0.14f, h, 0.12f), 0xFFFFD600, texture = SurfaceTexture.HAZARD_STRIPES)

        // Heavy Titanium Pressure Door with Circular Viewport
        addBox(Vector3(0f, h * 0.48f, z1), Vector3(w * 0.78f, h * 0.88f, 0.08f), 0xFF37474F, specular = 0.5f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0f, h * 0.65f, z1 + 0.05f), Vector3(0.42f, 0.42f, 0.04f), 0xFF80D8FF, specular = 0.95f, texture = SurfaceTexture.GREENHOUSE_GLASS)

        // Exterior EVA Access Terminal & Digital Keypad Console
        addBox(Vector3(w / 2 + 0.25f, 1.25f, z1 - 0.35f), Vector3(0.18f, 0.36f, 0.24f), 0xFF212121)
        addBox(Vector3(w / 2 + 0.35f, 1.30f, z1 - 0.35f), Vector3(0.04f, 0.18f, 0.14f), 0xFF00E5FF, emissive = true)
        addBox(Vector3(0f, h + 0.28f, z1), Vector3(0.32f, 0.14f, 0.14f), 0xFF00E676, emissive = true)

        return Mesh3D(vertices, faces, name = "HabitatDome")
    }

    // -------------------------------------------------------------
    // ARTICULATED PHOTOVOLTAIC TRACKING SOLAR ARRAY
    // -------------------------------------------------------------
    fun createSolarArray(tiltAngle: Float = 0.45f): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            rotX: Float = 0f,
            rotY: Float = 0f,
            rotZ: Float = 0f,
            emissive: Boolean = false,
            specular: Float = 0.3f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive, specular, texture)
        }

        // 1. Reinforced Ground Foundation Pier & Anchor Spikes
        addBox(Vector3(0f, 0.15f, 0f), Vector3(1.6f, 0.30f, 1.6f), 0xFF37474F, specular = 0.35f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0f, 0.35f, 0f), Vector3(0.95f, 0.20f, 0.95f), 0xFF263238)

        // 2. Open-Lattice Structural Truss Mast with Diagonal Cross-Members
        addBox(Vector3(0f, 1.65f, 0f), Vector3(0.40f, 2.50f, 0.40f), 0xFF455A64, specular = 0.4f)
        addBox(Vector3(0f, 1.20f, 0f), Vector3(0.65f, 0.08f, 0.65f), 0xFF607D8B)
        addBox(Vector3(0f, 2.10f, 0f), Vector3(0.65f, 0.08f, 0.65f), 0xFF607D8B)

        // 3. Motorized Dual-Axis Tracking Gimbal Head & Rotary Actuator
        addBox(Vector3(0f, 3.05f, 0f), Vector3(0.70f, 0.50f, 0.70f), 0xFF212121, specular = 0.5f)
        addBox(Vector3(0f, 3.05f, 0f), Vector3(0.24f, 0.24f, 1.20f), 0xFF78909C)

        // 4. Central Pivot Backbone Arm
        val cosT = cos(tiltAngle)
        val sinT = sin(tiltAngle)
        addBox(Vector3(0f, 3.05f, 0f), Vector3(5.6f, 0.20f, 0.22f), 0xFF37474F)

        // 5. Dual Split Photovoltaic Solar Wings (Port & Starboard Wings)
        fun addSolarWing(offsetX: Float, wingWidth: Float, wingHeight: Float) {
            val pBase = vertices.size
            val cy = 3.05f
            val halfW = wingWidth / 2f
            val halfH = wingHeight / 2f

            val corners = arrayOf(
                Vector3(offsetX - halfW, cy - halfH * sinT, -halfH * cosT),
                Vector3(offsetX + halfW, cy - halfH * sinT, -halfH * cosT),
                Vector3(offsetX + halfW, cy + halfH * sinT, halfH * cosT),
                Vector3(offsetX - halfW, cy + halfH * sinT, halfH * cosT)
            )
            for (c in corners) vertices.add(c)

            // Photovoltaic wafer front face with deep blue silicon coating and busbars
            faces.add(Face3D(pBase, pBase + 1, pBase + 2, pBase + 3, 0xFF0D47A1, doubleSided = true, specular = 0.95f, texture = SurfaceTexture.SOLAR_CELLS))
        }

        // Port Wing & Starboard Wing
        addSolarWing(offsetX = -1.65f, wingWidth = 2.45f, wingHeight = 2.40f)
        addSolarWing(offsetX = 1.65f, wingWidth = 2.45f, wingHeight = 2.40f)

        // 6. Ground Inverter Power Electronics Unit with Radiator Cooling Fins
        addBox(Vector3(0.65f, 0.55f, 0f), Vector3(0.55f, 0.65f, 0.55f), 0xFF263238, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0.95f, 0.65f, 0f), Vector3(0.06f, 0.40f, 0.45f), 0xFF455A64) // Radiator fins
        addBox(Vector3(0.65f, 0.85f, 0.28f), Vector3(0.12f, 0.12f, 0.04f), 0xFF00E676, emissive = true) // Power generation LED

        return Mesh3D(vertices, faces, name = "SolarArray")
    }

    // -------------------------------------------------------------
    // CELSS BIO-REGENERATIVE HYDROPONIC GREENHOUSE
    // -------------------------------------------------------------
    fun createGreenhouse(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()
        val length = 5.8f
        val width = 3.2f
        val height = 2.6f

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            emissive: Boolean = false,
            specular: Float = 0.25f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, emissive = emissive, specular = specular, texture = texture)
        }

        // 1. Heavy Composite Sill Foundation & Insulated Ground Base
        addBox(Vector3(0f, 0.22f, 0f), Vector3(width + 0.45f, 0.45f, length + 0.45f), 0xFF263238, specular = 0.35f, texture = SurfaceTexture.METAL_PANELS)

        // 2. Structural Quonset Rib Arches & Hermetic Glazing (Multi-Pane Radiation Shielded Glass)
        addBox(Vector3(0f, height * 0.55f + 0.2f, 0f), Vector3(width, height, length), 0xFF81C784, emissive = true, specular = 0.85f, texture = SurfaceTexture.GREENHOUSE_GLASS)

        // 3. Vaulted Transverse Rib Reinforcement Collars
        val ribCount = 4
        for (i in 0..ribCount) {
            val zPos = -length / 2f + (length / ribCount) * i
            addBox(Vector3(0f, height * 0.55f + 0.2f, zPos), Vector3(width + 0.16f, height + 0.16f, 0.24f), 0xFF37474F, specular = 0.4f, texture = SurfaceTexture.METAL_PANELS)
        }

        // 4. Interior Tiered Aeroponic Crop Trays & Photosynthetic LED Spectrum Illumination
        addBox(Vector3(0f, 0.65f, 0f), Vector3(width * 0.72f, 0.35f, length * 0.85f), 0xFF2E7D32, emissive = true) // Plant foliage
        addBox(Vector3(0f, 1.45f, 0f), Vector3(0.35f, 0.12f, length * 0.80f), 0xFFE040FB, emissive = true) // Magenta grow lights

        // 5. External Atmospheric CO2 Scrubber & Nutrient Recirculation Unit
        addBox(Vector3(width / 2 + 0.35f, 0.75f, 0.6f), Vector3(0.45f, 1.10f, 1.20f), 0xFF455A64, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(width / 2 + 0.35f, 1.15f, 0.6f), Vector3(0.24f, 0.24f, 0.24f), 0xFF00E5FF, emissive = true) // Digital flow meter

        return Mesh3D(vertices, faces, name = "Greenhouse")
    }

    // -------------------------------------------------------------
    // SABATIER OXYGEN & WATER ISRU REACTOR
    // -------------------------------------------------------------
    fun createOxygenGenerator(isDamaged: Boolean = false): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            emissive: Boolean = false,
            specular: Float = 0.25f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, emissive = emissive, specular = specular, texture = texture)
        }

        // 1. Heavy Transport Skid Chassis with Yellow-Black Hazard Striping
        addBox(Vector3(0f, 0.15f, 0f), Vector3(2.20f, 0.30f, 1.80f), 0xFF263238, texture = SurfaceTexture.HAZARD_STRIPES)

        // 2. Central Catalytic Reaction Chamber with Heat Dissipation Fins
        addBox(Vector3(0f, 1.15f, 0f), Vector3(1.10f, 1.70f, 1.10f), 0xFF37474F, specular = 0.45f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0f, 1.95f, 0f), Vector3(0.85f, 0.12f, 0.85f), 0xFF546E7A)
        addBox(Vector3(0f, 2.15f, 0f), Vector3(0.25f, 0.40f, 0.25f), 0xFF263238)

        // 3. Twin Cryogenic Liquid Oxygen & Water Storage Dewar Tanks
        addBox(Vector3(-0.65f, 1.10f, 0f), Vector3(0.60f, 1.60f, 0.60f), 0xFFB0BEC5, specular = 0.75f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(-0.65f, 1.45f, 0f), Vector3(0.66f, 0.14f, 0.66f), 0xFF00ACC1, specular = 0.8f)
        addBox(Vector3(0.65f, 1.10f, 0f), Vector3(0.60f, 1.60f, 0.60f), 0xFFB0BEC5, specular = 0.75f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0.65f, 1.45f, 0f), Vector3(0.66f, 0.14f, 0.66f), 0xFF00ACC1, specular = 0.8f)

        // 4. Atmospheric Intake Filter Grille & Turbocompressor Housing
        addBox(Vector3(0f, 0.85f, -0.65f), Vector3(0.75f, 0.65f, 0.35f), 0xFF212121)

        // 5. Digital Telemetry Console & Status Readout
        val panelColor = if (isDamaged) 0xFFFF1744 else 0xFF00E676
        addBox(Vector3(0f, 1.30f, 0.60f), Vector3(0.70f, 0.45f, 0.10f), panelColor, emissive = true)
        addBox(Vector3(0.25f, 0.90f, 0.60f), Vector3(0.12f, 0.25f, 0.10f), 0xFFFFB300)

        return Mesh3D(vertices, faces, name = "OxygenGenerator")
    }

    // -------------------------------------------------------------
    // DEEP SPACE COMMUNICATIONS NETWORK DISH
    // -------------------------------------------------------------
    fun createCommDish(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            rotX: Float = 0f,
            rotY: Float = 0f,
            rotZ: Float = 0f,
            emissive: Boolean = false,
            specular: Float = 0.25f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive, specular, texture)
        }

        // 1. Hexagonal Reinforced Pedestal Base & Transceiver Housing
        addBox(Vector3(0f, 0.25f, 0f), Vector3(2.4f, 0.50f, 2.4f), 0xFF37474F, specular = 0.35f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0f, 1.50f, 0f), Vector3(0.85f, 2.00f, 0.85f), 0xFF455A64, specular = 0.45f, texture = SurfaceTexture.METAL_PANELS)

        // 2. Motorized Azimuth/Elevation Gimbal & Counterweight Gearbox
        addBox(Vector3(0f, 2.75f, 0f), Vector3(1.10f, 0.60f, 1.10f), 0xFF263238, specular = 0.5f)
        addBox(Vector3(0f, 2.75f, -0.65f), Vector3(0.75f, 0.50f, 0.55f), 0xFF212121)

        // 3. Concave Multi-Segmented Parabolic Dish Reflector
        val dishRadius = 2.4f
        val dishSegments = 12
        val dishBase = vertices.size
        val dishCenter = Vector3(0f, 4.20f, 0.45f)
        val dishTilt = 0.52f
        val cosT = cos(dishTilt)
        val sinT = sin(dishTilt)

        vertices.add(dishCenter)
        for (i in 0 until dishSegments) {
            val th = (i.toFloat() / dishSegments) * (2f * PI.toFloat())
            val rx = dishRadius * cos(th)
            val ry = dishRadius * sin(th)
            val rz = -(rx * rx + ry * ry) * 0.08f
            val tiltedY = ry * cosT - rz * sinT
            val tiltedZ = ry * sinT + rz * cosT
            vertices.add(dishCenter + Vector3(rx, tiltedY, tiltedZ))
        }

        for (i in 0 until dishSegments) {
            val next = (i + 1) % dishSegments
            val v0 = dishBase
            val v1 = dishBase + 1 + i
            val v2 = dishBase + 1 + next
            faces.add(Face3D(v0, v1, v2, baseColor = 0xFFECEFF1, doubleSided = true, specular = 0.85f, texture = SurfaceTexture.METAL_PANELS))
        }

        // 4. Quadripod Feed Horn Strut Assembly & Central Sub-Reflector
        val hornPos = dishCenter + Vector3(0f, 0.95f * cosT, 0.95f * sinT)
        addBox(hornPos, Vector3(0.35f, 0.35f, 0.45f), 0xFF212121, rotX = dishTilt, specular = 0.6f)
        addBox(hornPos + Vector3(0f, 0.15f * cosT, 0.15f * sinT), Vector3(0.18f, 0.18f, 0.18f), 0xFF00E5FF, emissive = true)

        // 5. Warning Aircraft-Avoidance Red Flashing Beacon on Apex
        addBox(dishCenter + Vector3(0f, dishRadius * cosT + 0.35f, dishRadius * sinT), Vector3(0.18f, 0.28f, 0.18f), 0xFFFF1744, emissive = true)

        return Mesh3D(vertices, faces, name = "CommDish")
    }

    // -------------------------------------------------------------
    // SPACECRAFT DESCENT & ASCENT EXPEDITION LANDER
    // -------------------------------------------------------------
    fun createSpaceLander(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(
            center: Vector3,
            size: Vector3,
            color: Long,
            rotX: Float = 0f,
            rotY: Float = 0f,
            rotZ: Float = 0f,
            emissive: Boolean = false,
            specular: Float = 0.35f,
            texture: SurfaceTexture = SurfaceTexture.NONE
        ) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive, specular, texture)
        }

        // 1. Octagonal Descent Stage wrapped in Crinkled Gold MLI Thermal Blanketing
        addBox(Vector3(0f, 2.45f, 0f), Vector3(4.50f, 2.20f, 4.50f), 0xFFFFD700, specular = 0.95f, texture = SurfaceTexture.THERMAL_GOLD_FOIL)
        addBox(Vector3(0f, 2.45f, 0f), Vector3(3.80f, 2.25f, 4.80f), 0xFFFFD700, specular = 0.95f, texture = SurfaceTexture.THERMAL_GOLD_FOIL)

        // 2. Central Rocket Propulsion Engine with Deep Bell Nozzle & Heat Shield
        addBox(Vector3(0f, 0.75f, 0f), Vector3(1.60f, 1.40f, 1.60f), 0xFF1C1C1C, specular = 0.6f)
        addBox(Vector3(0f, 0.15f, 0f), Vector3(1.85f, 0.20f, 1.85f), 0xFF37474F)

        // 3. Upper Pressurized Crew Cabin with Observation Viewports & Egress Hatch
        addBox(Vector3(0f, 4.65f, 0f), Vector3(3.30f, 2.30f, 3.30f), 0xFFECEFF1, specular = 0.5f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(-0.65f, 4.90f, 1.68f), Vector3(0.55f, 0.40f, 0.08f), 0xFF80D8FF, specular = 0.95f, texture = SurfaceTexture.GREENHOUSE_GLASS)
        addBox(Vector3(0.65f, 4.90f, 1.68f), Vector3(0.55f, 0.40f, 0.08f), 0xFF80D8FF, specular = 0.95f, texture = SurfaceTexture.GREENHOUSE_GLASS)
        addBox(Vector3(0f, 4.35f, 1.68f), Vector3(0.95f, 1.25f, 0.08f), 0xFFFFD600, texture = SurfaceTexture.HAZARD_STRIPES)

        // 4. Articulated Quad Landing Gear Assemblies with Primary Shock Struts & Footpads
        val legAngles = listOf(0.785f, 2.356f, 3.926f, 5.497f)
        for ((idx, ang) in legAngles.withIndex()) {
            val dist = 3.65f
            val footX = cos(ang) * dist
            val footZ = sin(ang) * dist

            addBox(Vector3(footX * 0.55f, 1.30f, footZ * 0.55f), Vector3(0.24f, 2.20f, 0.24f), 0xFF78909C, specular = 0.6f)
            addBox(Vector3(footX * 0.75f, 0.85f, footZ * 0.75f), Vector3(0.14f, 1.40f, 0.14f), 0xFF546E7A)
            addBox(Vector3(footX, 0.12f, footZ), Vector3(1.10f, 0.18f, 1.10f), 0xFFFFD700, specular = 0.95f, texture = SurfaceTexture.THERMAL_GOLD_FOIL)

            if (idx == 0) {
                for (step in 0..6) {
                    val stepY = 0.35f + step * 0.32f
                    val stepFrac = stepY / 2.3f
                    val stepX = footX * (1f - stepFrac * 0.5f)
                    val stepZ = footZ * (1f - stepFrac * 0.5f) + 0.15f
                    addBox(Vector3(stepX, stepY, stepZ), Vector3(0.38f, 0.05f, 0.08f), 0xFFFFB300)
                }
            }
        }

        // 5. Reaction Control System (RCS) Quad Thruster Blocks on Hull Corners
        val rcsOffsets = listOf(
            Vector3(-1.75f, 3.40f, -1.75f),
            Vector3(1.75f, 3.40f, -1.75f),
            Vector3(1.75f, 3.40f, 1.75f),
            Vector3(-1.75f, 3.40f, 1.75f)
        )
        for (rcs in rcsOffsets) {
            addBox(rcs, Vector3(0.30f, 0.30f, 0.30f), 0xFF37474F)
            addBox(rcs + Vector3(0f, 0.12f, 0f), Vector3(0.12f, 0.18f, 0.12f), 0xFF1C1C1C)
        }

        // 6. External Spherical Helium Pressurant Tanks & Steerable Comm Dish
        addBox(Vector3(-1.95f, 2.50f, 0f), Vector3(0.85f, 0.85f, 0.85f), 0xFFB0BEC5, specular = 0.85f)
        addBox(Vector3(1.95f, 2.50f, 0f), Vector3(0.85f, 0.85f, 0.85f), 0xFFB0BEC5, specular = 0.85f)
        addBox(Vector3(-1.10f, 6.10f, -0.90f), Vector3(0.70f, 0.70f, 0.12f), 0xFFECEFF1, specular = 0.8f, texture = SurfaceTexture.METAL_PANELS)

        return Mesh3D(vertices, faces, name = "SpaceLander")
    }

    // -------------------------------------------------------------
    // NATURAL CRYSTALLINE MINERAL & WATER-ICE DEPOSIT
    // -------------------------------------------------------------
    fun createMineralDeposit(type: String = "ice"): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        val primaryColor = when (type) {
            "ice" -> 0xFF80DEEA
            "hematite" -> 0xFFFF7043
            "rare_earth" -> 0xFFFFD700
            else -> 0xFFB0BEC5
        }
        val secondaryColor = when (type) {
            "ice" -> 0xFF00E5FF
            "hematite" -> 0xFFD84315
            "rare_earth" -> 0xFFFFA000
            else -> 0xFF78909C
        }

        fun addCrystalColumn(center: Vector3, radius: Float, height: Float, tiltX: Float = 0f, tiltZ: Float = 0f) {
            val cb = vertices.size
            val segs = 6
            for (i in 0 until segs) {
                val th = (i.toFloat() / segs) * (2f * PI.toFloat())
                val local = Vector3(radius * cos(th), 0f, radius * sin(th)).rotateX(tiltX).rotateZ(tiltZ)
                vertices.add(center + local)
            }
            for (i in 0 until segs) {
                val th = (i.toFloat() / segs) * (2f * PI.toFloat())
                val local = Vector3(radius * 0.9f * cos(th), height * 0.75f, radius * 0.9f * sin(th)).rotateX(tiltX).rotateZ(tiltZ)
                vertices.add(center + local)
            }
            val tip = center + Vector3(0f, height, 0f).rotateX(tiltX).rotateZ(tiltZ)
            vertices.add(tip)

            for (i in 0 until segs) {
                val next = (i + 1) % segs
                val b0 = cb + i
                val b1 = cb + next
                val m1 = cb + segs + next
                val m0 = cb + segs + i
                val col = if (i % 2 == 0) primaryColor else secondaryColor
                faces.add(Face3D(b0, b1, m1, m0, col, emissive = (type == "ice"), doubleSided = true, specular = 0.95f))
            }
            val tipIdx = cb + segs * 2
            for (i in 0 until segs) {
                val next = (i + 1) % segs
                val m0 = cb + segs + i
                val m1 = cb + segs + next
                faces.add(Face3D(m0, m1, tipIdx, baseColor = primaryColor, emissive = (type == "ice"), doubleSided = true, specular = 0.95f))
            }
        }

        addBoxToMesh(vertices, faces, Vector3(0f, 0.15f, 0f), Vector3(2.20f, 0.30f, 2.20f), 0xFF37474F, specular = 0.15f, texture = SurfaceTexture.CRATER_ROCK)
        addCrystalColumn(Vector3(0f, 0.25f, 0f), radius = 0.38f, height = 1.45f, tiltX = 0.08f, tiltZ = -0.06f)
        addCrystalColumn(Vector3(0.45f, 0.22f, 0.25f), radius = 0.28f, height = 1.10f, tiltX = 0.22f, tiltZ = 0.18f)
        addCrystalColumn(Vector3(-0.40f, 0.20f, -0.30f), radius = 0.32f, height = 1.25f, tiltX = -0.15f, tiltZ = -0.25f)
        addCrystalColumn(Vector3(-0.35f, 0.18f, 0.40f), radius = 0.24f, height = 0.85f, tiltX = 0.18f, tiltZ = -0.20f)
        addCrystalColumn(Vector3(0.50f, 0.15f, -0.35f), radius = 0.22f, height = 0.75f, tiltX = -0.25f, tiltZ = 0.20f)

        return Mesh3D(vertices, faces, name = "Mineral_$type")
    }

    // -------------------------------------------------------------
    // AERIAL SCOUT / SCIENCE QUAD DRONE
    // -------------------------------------------------------------
    fun createDroneMesh(rotorAngle: Float = 0f, tiltX: Float = 0f, tiltZ: Float = 0f): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(center: Vector3, size: Vector3, color: Long, rotX: Float = 0f, rotY: Float = 0f, rotZ: Float = 0f, emissive: Boolean = false) {
            addBoxToMesh(vertices, faces, center, size, color, rotX, rotY, rotZ, emissive = emissive)
        }

        addBox(Vector3(0f, 0.2f, 0f), Vector3(0.42f, 0.18f, 0.42f), 0xFF263238, rotX = tiltX, rotZ = tiltZ)
        addBox(Vector3(0f, 0.08f, 0.20f), Vector3(0.16f, 0.16f, 0.16f), 0xFF00E5FF, rotX = tiltX, rotZ = tiltZ, emissive = true)

        val armOffsets = listOf(
            Vector3(-0.38f, 0.24f, -0.38f),
            Vector3(0.38f, 0.24f, -0.38f),
            Vector3(-0.38f, 0.24f, 0.38f),
            Vector3(0.38f, 0.24f, 0.38f)
        )
        for (arm in armOffsets) {
            val armPos = arm.rotateX(tiltX).rotateZ(tiltZ)
            addBox(armPos * 0.5f, Vector3(0.06f, 0.04f, 0.55f), 0xFF455A64, rotX = tiltX, rotZ = tiltZ)
            addBox(armPos, Vector3(0.12f, 0.10f, 0.12f), 0xFF212121, rotX = tiltX, rotZ = tiltZ)
            addBox(armPos + Vector3(0f, 0.06f, 0f), Vector3(0.48f, 0.02f, 0.06f), 0xFF90A4AE, rotY = rotorAngle)
        }

        return Mesh3D(vertices, faces, name = "ScoutDrone")
    }

    // -------------------------------------------------------------
    // PRESSURIZED UTILITY CONDUIT / CORRIDOR TUNNEL
    // -------------------------------------------------------------
    fun createUtilityConduitTunnel(length: Float = 6.0f, width: Float = 1.4f, height: Float = 1.6f): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(center: Vector3, size: Vector3, color: Long, emissive: Boolean = false, specular: Float = 0.3f, texture: SurfaceTexture = SurfaceTexture.NONE) {
            addBoxToMesh(vertices, faces, center, size, color, emissive = emissive, specular = specular, texture = texture)
        }

        // Main Conduit Tunnel Tube
        addBox(Vector3(0f, height / 2f, 0f), Vector3(width, height, length), 0xFFECEFF1, specular = 0.45f, texture = SurfaceTexture.METAL_PANELS)

        // Corrugated Structural Collars & Overhead LED Guide Strip
        val ribCount = (length / 1.4f).toInt().coerceAtLeast(2)
        for (i in 0..ribCount) {
            val zPos = -length / 2f + (length / ribCount) * i
            addBox(Vector3(0f, height / 2f, zPos), Vector3(width + 0.18f, height + 0.18f, 0.28f), 0xFF37474F, specular = 0.35f, texture = SurfaceTexture.METAL_PANELS)
            addBox(Vector3(0f, height + 0.14f, zPos), Vector3(0.14f, 0.08f, 0.14f), 0xFF00E5FF, emissive = true)
        }

        return Mesh3D(vertices, faces, name = "ConduitTunnel")
    }

    // -------------------------------------------------------------
    // PERIMETER TELEMETRY NAVIGATION BEACON PYLON
    // -------------------------------------------------------------
    fun createPerimeterBeaconPylon(beaconColor: Long = 0xFF00E5FF): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(center: Vector3, size: Vector3, color: Long, emissive: Boolean = false, specular: Float = 0.25f, texture: SurfaceTexture = SurfaceTexture.NONE) {
            addBoxToMesh(vertices, faces, center, size, color, emissive = emissive, specular = specular, texture = texture)
        }

        // Weighted Tripod Base Staked into Regolith
        addBox(Vector3(0f, 0.15f, 0f), Vector3(0.95f, 0.30f, 0.95f), 0xFF37474F, specular = 0.35f, texture = SurfaceTexture.METAL_PANELS)
        // Telescopic Mast
        addBox(Vector3(0f, 1.85f, 0f), Vector3(0.16f, 3.40f, 0.16f), 0xFFECEFF1, specular = 0.6f)
        // Solar Recharging Wafer Cap
        addBox(Vector3(0f, 3.60f, 0f), Vector3(0.55f, 0.08f, 0.55f), 0xFF0D47A1, specular = 0.85f, texture = SurfaceTexture.SOLAR_CELLS)
        // High-Intensity Fresnel Beacon Strobe Lens
        addBox(Vector3(0f, 3.82f, 0f), Vector3(0.28f, 0.34f, 0.28f), beaconColor, emissive = true)

        return Mesh3D(vertices, faces, name = "BeaconPylon")
    }

    // -------------------------------------------------------------
    // CRYOGENIC RESOURCE STORAGE TANK CLUSTER
    // -------------------------------------------------------------
    fun createCryoStorageUnit(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(center: Vector3, size: Vector3, color: Long, emissive: Boolean = false, specular: Float = 0.35f, texture: SurfaceTexture = SurfaceTexture.NONE) {
            addBoxToMesh(vertices, faces, center, size, color, emissive = emissive, specular = specular, texture = texture)
        }

        // Structural Support Skid Cradle with Tie-Down Mounts
        addBox(Vector3(0f, 0.18f, 0f), Vector3(2.6f, 0.36f, 2.0f), 0xFF263238, texture = SurfaceTexture.HAZARD_STRIPES)

        // Large Vacuum-Jacketed Spherical Dewar Vessel (Gold Thermal MLI Wrap)
        addBox(Vector3(-0.65f, 1.25f, 0f), Vector3(1.30f, 1.70f, 1.30f), 0xFFFFD700, specular = 0.95f, texture = SurfaceTexture.THERMAL_GOLD_FOIL)

        // Horizontal Pressurized Cryo Cylinder (White Thermal Barrier Coating)
        addBox(Vector3(0.65f, 1.15f, 0f), Vector3(1.10f, 1.50f, 1.50f), 0xFFECEFF1, specular = 0.65f, texture = SurfaceTexture.METAL_PANELS)
        addBox(Vector3(0.65f, 1.15f, 0f), Vector3(1.18f, 0.15f, 1.56f), 0xFF00ACC1) // Blue inspection band

        // Manifold Cabinet & Pressure Relief Vent Stack
        addBox(Vector3(0f, 1.35f, 0.65f), Vector3(0.95f, 0.55f, 0.25f), 0xFF00E5FF, emissive = true) // Flow telemetry
        addBox(Vector3(0f, 2.15f, 0f), Vector3(0.12f, 0.60f, 0.12f), 0xFFFFB300) // Relief stack

        return Mesh3D(vertices, faces, name = "CryoStorage")
    }

    // -------------------------------------------------------------
    // PLANETARY FIELD SCIENCE SURVEY STATION RACK
    // -------------------------------------------------------------
    fun createScienceFieldRack(): Mesh3D {
        val vertices = mutableListOf<Vector3>()
        val faces = mutableListOf<Face3D>()

        fun addBox(center: Vector3, size: Vector3, color: Long, emissive: Boolean = false, specular: Float = 0.3f, texture: SurfaceTexture = SurfaceTexture.NONE) {
            addBoxToMesh(vertices, faces, center, size, color, emissive = emissive, specular = specular, texture = texture)
        }

        // Instrument Platform Table on Ground Mounts
        addBox(Vector3(0f, 0.50f, 0f), Vector3(2.0f, 1.00f, 1.30f), 0xFF455A64, specular = 0.4f, texture = SurfaceTexture.METAL_PANELS)

        // Seismometer Dome with Gold MLI Thermal Shroud
        addBox(Vector3(-0.55f, 1.15f, 0.15f), Vector3(0.60f, 0.40f, 0.60f), 0xFFFFD700, specular = 0.95f, texture = SurfaceTexture.THERMAL_GOLD_FOIL)

        // Mass Spectrometer & Telemetry Terminal
        addBox(Vector3(0.40f, 1.15f, 0f), Vector3(0.65f, 0.45f, 0.50f), 0xFF00E5FF, emissive = true)

        // Miniature Radioisotope Thermoelectric Generator (RTG) with Radial Heat Fins
        addBox(Vector3(-0.45f, 0.50f, -0.75f), Vector3(0.50f, 0.65f, 0.50f), 0xFF212121)
        addBox(Vector3(-0.45f, 0.50f, -0.75f), Vector3(0.65f, 0.50f, 0.65f), 0xFF37474F)

        // Meteorological & Radiation Sensor Mast
        addBox(Vector3(0.70f, 1.75f, -0.40f), Vector3(0.06f, 1.60f, 0.06f), 0xFFECEFF1)
        addBox(Vector3(0.70f, 2.58f, -0.40f), Vector3(0.12f, 0.12f, 0.12f), 0xFF00E676, emissive = true)

        return Mesh3D(vertices, faces, name = "ScienceRack")
    }
}
