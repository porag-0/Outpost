package com.example.game.engine3d

import kotlin.math.*

object CollisionSystem {

    sealed class Obstacle {
        abstract val name: String

        data class Circle(
            val x: Float,
            val z: Float,
            val radius: Float,
            override val name: String = ""
        ) : Obstacle()

        data class Box(
            val x: Float,
            val z: Float,
            val halfWidth: Float,
            val halfLength: Float,
            val rotationY: Float = 0f,
            override val name: String = ""
        ) : Obstacle()
    }

    private val worldObstacles = mutableListOf<Obstacle>()

    init {
        initObstacles()
    }

    fun initObstacles() {
        worldObstacles.clear()

        // 1. Habitat Geodesic Command Dome (center 0,0, radius 5.1m)
        worldObstacles.add(Obstacle.Circle(0f, 0f, 5.1f, "HabitatDome"))
        // Airlock side walls flanking the approach corridor (Z from 5.1m to 7.8m)
        worldObstacles.add(Obstacle.Box(-1.05f, 6.5f, 0.22f, 1.3f, 0f, "AirlockWallLeft"))
        worldObstacles.add(Obstacle.Box(1.05f, 6.5f, 0.22f, 1.3f, 0f, "AirlockWallRight"))

        // 2. Pressurized Utility Conduit Corridors connecting modules
        worldObstacles.add(Obstacle.Box(7.2f, 3.0f, 0.95f, 3.85f, -0.42f, "CorridorGreenhouse"))
        worldObstacles.add(Obstacle.Box(4.5f, -2.5f, 0.95f, 2.65f, 0.52f, "CorridorOxygen"))

        // 3. Dual Photovoltaic Solar Arrays
        worldObstacles.add(Obstacle.Box(-14f, 10f, 2.7f, 1.4f, 0.4f, "SolarArray1"))
        worldObstacles.add(Obstacle.Box(-14f, 18f, 2.7f, 1.4f, 0.4f, "SolarArray2"))

        // 4. CELSS Aeroponic Greenhouse
        worldObstacles.add(Obstacle.Box(14f, 6f, 1.9f, 3.3f, -0.3f, "Greenhouse"))

        // 5. Sabatier Oxygen & Water Reactor
        worldObstacles.add(Obstacle.Box(9f, -5f, 1.35f, 1.25f, 0f, "OxygenGen"))

        // 6. Communications Deep Space Network Dish
        worldObstacles.add(Obstacle.Circle(-12f, -12f, 2.2f, "CommDish"))

        // 7. Spacecraft Lander
        worldObstacles.add(Obstacle.Circle(-24f, 4f, 3.8f, "SpaceLander"))

        // 8. Cryogenic Propellant & Life Support Storage Cluster
        worldObstacles.add(Obstacle.Box(-17f, -2.5f, 1.65f, 1.35f, 0.35f, "CryoStorage"))

        // 9. Science Field Survey Station Rack
        worldObstacles.add(Obstacle.Box(18.5f, 19.5f, 1.25f, 0.95f, -0.6f, "ScienceRack"))

        // 10. Water-Ice Deposit in South Crater
        worldObstacles.add(Obstacle.Circle(22f, 25f, 1.35f, "IceDeposit"))

        // 11. Hematite / Mineral Outcrop
        worldObstacles.add(Obstacle.Circle(-26f, -18f, 1.35f, "HematiteDeposit"))

        // 12. Perimeter Navigation Beacon Pylons (7 Waypoints)
        val beaconWaypoints = listOf(
            Vector3(-18f, 0f, 5f),
            Vector3(-6f, 0f, 21f),
            Vector3(12f, 0f, 17f),
            Vector3(18f, 0f, 22f),
            Vector3(25f, 0f, 25f),
            Vector3(-14f, 0f, -14f),
            Vector3(13f, 0f, -8f)
        )
        for ((idx, b) in beaconWaypoints.withIndex()) {
            worldObstacles.add(Obstacle.Circle(b.x, b.z, 0.55f, "Beacon_$idx"))
        }

        // 13. Natural Planetary Boulders (all 12 boulders from world generator)
        val boulderConfigs = listOf(
            Pair(Vector3(18f, 0f, 19f), 1.35f),
            Pair(Vector3(27f, 0f, 21f), 1.75f),
            Pair(Vector3(-21f, 0f, -14f), 1.35f),
            Pair(Vector3(-32f, 0f, -22f), 2.15f),
            Pair(Vector3(29f, 0f, -8f), 1.55f),
            Pair(Vector3(8f, 0f, -17f), 1.15f),
            Pair(Vector3(-8f, 0f, 23f), 1.25f),
            Pair(Vector3(-19f, 0f, 27f), 1.65f),
            Pair(Vector3(24f, 0f, 15f), 1.45f),
            Pair(Vector3(-15f, 0f, -9f), 1.15f),
            Pair(Vector3(14f, 0f, 28f), 1.65f),
            Pair(Vector3(-28f, 0f, 12f), 1.45f)
        )
        for ((idx, b) in boulderConfigs.withIndex()) {
            worldObstacles.add(Obstacle.Circle(b.first.x, b.first.z, b.second, "Boulder_$idx"))
        }

        // 14. Outpost Personnel NPCs (prevent player from clipping through crew members)
        worldObstacles.add(Obstacle.Circle(1.6f, 6.2f, 0.45f, "NPC_Vance"))
        worldObstacles.add(Obstacle.Circle(13.2f, 4.2f, 0.45f, "NPC_Thorne"))
        worldObstacles.add(Obstacle.Circle(8.0f, -3.8f, 0.45f, "NPC_Cole"))
        worldObstacles.add(Obstacle.Circle(-4.0f, 8.5f, 0.45f, "NPC_Lin"))
        worldObstacles.add(Obstacle.Circle(11.5f, 5.8f, 0.45f, "NPC_Turner"))
    }

    /**
     * Resolves continuous movement from (startX, startZ) to (desiredX, desiredZ)
     * using axis-separated collision resolution and sliding response.
     * Prevents penetration through all solid objects while allowing smooth gliding along walls/surfaces.
     */
    fun resolvePosition(
        startX: Float,
        startZ: Float,
        desiredX: Float,
        desiredZ: Float,
        entityRadius: Float,
        extraObstacles: List<Obstacle> = emptyList()
    ): Pair<Float, Float> {
        var currentX = startX
        var currentZ = startZ

        // Step 1: Attempt movement along X axis first
        var testX = desiredX
        var testZ = currentZ
        for (obs in worldObstacles) {
            val (rx, rz) = resolveSingle(testX, testZ, obs, entityRadius)
            testX = rx
            testZ = rz
        }
        for (obs in extraObstacles) {
            val (rx, rz) = resolveSingle(testX, testZ, obs, entityRadius)
            testX = rx
            testZ = rz
        }
        currentX = testX

        // Step 2: Attempt movement along Z axis
        testX = currentX
        testZ = desiredZ
        for (obs in worldObstacles) {
            val (rx, rz) = resolveSingle(testX, testZ, obs, entityRadius)
            testX = rx
            testZ = rz
        }
        for (obs in extraObstacles) {
            val (rx, rz) = resolveSingle(testX, testZ, obs, entityRadius)
            testX = rx
            testZ = rz
        }
        currentZ = testZ

        // Step 3: Relaxation passes to resolve any residual penetration at acute angles or junctions
        for (pass in 0 until 2) {
            for (obs in worldObstacles) {
                val (rx, rz) = resolveSingle(currentX, currentZ, obs, entityRadius)
                currentX = rx
                currentZ = rz
            }
            for (obs in extraObstacles) {
                val (rx, rz) = resolveSingle(currentX, currentZ, obs, entityRadius)
                currentX = rx
                currentZ = rz
            }
        }

        // Step 4: World perimeter boundary enforcement
        val boundary = 50.0f
        currentX = currentX.coerceIn(-boundary, boundary)
        currentZ = currentZ.coerceIn(-boundary, boundary)

        return Pair(currentX, currentZ)
    }

    private fun resolveSingle(
        px: Float,
        pz: Float,
        obstacle: Obstacle,
        entityRadius: Float
    ): Pair<Float, Float> {
        return when (obstacle) {
            is Obstacle.Circle -> resolveCircle(px, pz, obstacle.x, obstacle.z, obstacle.radius, entityRadius)
            is Obstacle.Box -> resolveBox(
                px, pz,
                obstacle.x, obstacle.z,
                obstacle.halfWidth, obstacle.halfLength,
                obstacle.rotationY,
                entityRadius
            )
        }
    }

    private fun resolveCircle(
        px: Float,
        pz: Float,
        cx: Float,
        cz: Float,
        obstacleRadius: Float,
        entityRadius: Float
    ): Pair<Float, Float> {
        val totalR = obstacleRadius + entityRadius
        val dx = px - cx
        val dz = pz - cz
        val distSq = dx * dx + dz * dz

        if (distSq < totalR * totalR) {
            val dist = sqrt(distSq)
            return if (dist > 0.0001f) {
                val nx = dx / dist
                val nz = dz / dist
                Pair(cx + nx * totalR, cz + nz * totalR)
            } else {
                Pair(cx + totalR, cz)
            }
        }
        return Pair(px, pz)
    }

    private fun resolveBox(
        px: Float,
        pz: Float,
        cx: Float,
        cz: Float,
        halfW: Float,
        halfL: Float,
        rotY: Float,
        entityRadius: Float
    ): Pair<Float, Float> {
        val cosR = cos(-rotY)
        val sinR = sin(-rotY)
        val dx = px - cx
        val dz = pz - cz
        val localX = dx * cosR - dz * sinR
        val localZ = dx * sinR + dz * cosR

        val clampedX = localX.coerceIn(-halfW, halfW)
        val clampedZ = localZ.coerceIn(-halfL, halfL)

        val diffX = localX - clampedX
        val diffZ = localZ - clampedZ
        val distSq = diffX * diffX + diffZ * diffZ

        val cosW = cos(rotY)
        val sinW = sin(rotY)

        if (distSq < 0.000001f) {
            // Point is strictly inside box volume: push outward to nearest face
            val penLeft = localX - (-halfW)
            val penRight = halfW - localX
            val penBack = localZ - (-halfL)
            val penFront = halfL - localZ
            val minPen = minOf(penLeft, penRight, penBack, penFront)

            val newLocalX = when (minPen) {
                penLeft -> -halfW - entityRadius
                penRight -> halfW + entityRadius
                else -> localX
            }
            val newLocalZ = when (minPen) {
                penBack -> -halfL - entityRadius
                penFront -> halfL + entityRadius
                else -> localZ
            }
            return Pair(cx + newLocalX * cosW - newLocalZ * sinW, cz + newLocalX * sinW + newLocalZ * cosW)
        }

        if (distSq < entityRadius * entityRadius) {
            val dist = sqrt(distSq)
            val nx = diffX / dist
            val nz = diffZ / dist
            val newLocalX = clampedX + nx * entityRadius
            val newLocalZ = clampedZ + nz * entityRadius
            return Pair(cx + newLocalX * cosW - newLocalZ * sinW, cz + newLocalX * sinW + newLocalZ * cosW)
        }

        return Pair(px, pz)
    }
}
