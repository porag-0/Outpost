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

        // 1. Habitat Geodesic Command Dome (center 0,0, radius 5.65m)
        worldObstacles.add(Obstacle.Circle(0f, 0f, 5.65f, "HabitatDome"))
        // Pressurized Outer Airlock Vestibule & Heavy Pressure Door (covers z from 5.2m to 8.25m, width 2.4m)
        worldObstacles.add(Obstacle.Box(0f, 6.72f, 1.25f, 1.55f, 0f, "AirlockModule"))

        // 2. Pressurized Utility Conduit Corridors connecting modules
        // Corridor to Greenhouse: center (7.2f, 3.0f), length 7.5m, width 1.4m + collars, rotY = -0.42f
        worldObstacles.add(Obstacle.Box(7.2f, 3.0f, 1.05f, 3.85f, -0.42f, "CorridorGreenhouse"))
        // Corridor to Oxygen Generator: center (4.5f, -2.5f), length 5.0m, width 1.4m + collars, rotY = 0.52f
        worldObstacles.add(Obstacle.Box(4.5f, -2.5f, 1.05f, 2.65f, 0.52f, "CorridorOxygen"))

        // 3. Dual Photovoltaic Solar Arrays
        // Solar Array 1: center (-14f, 10f), width 5.6m, depth 2.2m, rotY = 0.4f
        worldObstacles.add(Obstacle.Box(-14f, 10f, 2.95f, 1.35f, 0.4f, "SolarArray1"))
        // Solar Array 2: center (-14f, 18f), width 5.6m, depth 2.2m, rotY = 0.4f
        worldObstacles.add(Obstacle.Box(-14f, 18f, 2.95f, 1.35f, 0.4f, "SolarArray2"))

        // 4. CELSS Aeroponic Greenhouse: center (14f, 6f), width 3.2m, length 5.8m, rotY = -0.3f
        worldObstacles.add(Obstacle.Box(14f, 6f, 1.95f, 3.35f, -0.3f, "Greenhouse"))

        // 5. Sabatier Oxygen & Water Reactor: center (9f, -5f), width 2.2m, length 1.8m
        worldObstacles.add(Obstacle.Box(9f, -5f, 1.40f, 1.25f, 0f, "OxygenGen"))

        // 6. Communications Deep Space Network Dish: center (-12f, -12f), dish radius 2.4m + pedestal
        worldObstacles.add(Obstacle.Circle(-12f, -12f, 2.65f, "CommDish"))

        // 7. Spacecraft Lander: center (-24f, 4f), landing stage & 4 articulated struts extending to 4.2m
        worldObstacles.add(Obstacle.Circle(-24f, 4f, 4.35f, "SpaceLander"))

        // 8. Cryogenic Propellant & Life Support Storage: center (-17f, -2.5f), rotY = 0.35f
        worldObstacles.add(Obstacle.Box(-17f, -2.5f, 1.75f, 1.45f, 0.35f, "CryoStorage"))

        // 9. Science Field Survey Station Rack: center (18.5f, 19.5f), rotY = -0.6f
        worldObstacles.add(Obstacle.Box(18.5f, 19.5f, 1.40f, 1.05f, -0.6f, "ScienceRack"))

        // 10. Water-Ice Deposit in South Crater: center (22f, 25f)
        worldObstacles.add(Obstacle.Circle(22f, 25f, 1.85f, "IceDeposit"))

        // 11. Hematite / Mineral Outcrop: center (-26f, -18f)
        worldObstacles.add(Obstacle.Circle(-26f, -18f, 1.85f, "HematiteDeposit"))

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
            worldObstacles.add(Obstacle.Circle(b.x, b.z, 0.65f, "Beacon_$idx"))
        }

        // 13. Natural Planetary Boulders (all 12 boulders matching MeshFactory/GameViewModel)
        val boulderConfigs = listOf(
            Pair(Vector3(18f, 0f, 19f), 1.70f),
            Pair(Vector3(27f, 0f, 21f), 2.25f),
            Pair(Vector3(-21f, 0f, -14f), 1.75f),
            Pair(Vector3(-32f, 0f, -22f), 2.65f),
            Pair(Vector3(29f, 0f, -8f), 1.95f),
            Pair(Vector3(8f, 0f, -17f), 1.40f),
            Pair(Vector3(-8f, 0f, 23f), 1.60f),
            Pair(Vector3(-19f, 0f, 27f), 2.05f),
            Pair(Vector3(24f, 0f, 15f), 1.85f),
            Pair(Vector3(-15f, 0f, -9f), 1.50f),
            Pair(Vector3(14f, 0f, 28f), 2.15f),
            Pair(Vector3(-28f, 0f, 12f), 1.95f)
        )
        for ((idx, b) in boulderConfigs.withIndex()) {
            worldObstacles.add(Obstacle.Circle(b.first.x, b.first.z, b.second, "Boulder_$idx"))
        }

        // 14. Outpost Personnel Crew NPCs
        worldObstacles.add(Obstacle.Circle(1.6f, 6.2f, 0.55f, "NPC_Vance"))
        worldObstacles.add(Obstacle.Circle(13.2f, 4.2f, 0.55f, "NPC_Thorne"))
        worldObstacles.add(Obstacle.Circle(8.0f, -3.8f, 0.55f, "NPC_Cole"))
        worldObstacles.add(Obstacle.Circle(-4.0f, 8.5f, 0.55f, "NPC_Lin"))
        worldObstacles.add(Obstacle.Circle(11.5f, 5.8f, 0.55f, "NPC_Turner"))
    }

    /**
     * Continuous sub-stepped and iterative sliding collision resolution.
     * Prevents penetration through all solid objects (circular or OBB boxes),
     * completely eliminating tunneling while providing smooth, fluid sliding along surfaces.
     */
    fun resolvePosition(
        startX: Float,
        startZ: Float,
        desiredX: Float,
        desiredZ: Float,
        entityRadius: Float,
        extraObstacles: List<Obstacle> = emptyList()
    ): Pair<Float, Float> {
        val allObstacles = if (extraObstacles.isEmpty()) worldObstacles else worldObstacles + extraObstacles

        // 1. Ensure start position is not already penetrating any obstacle
        var curX = startX
        var curZ = startZ
        for (obs in allObstacles) {
            val (px, pz, hit) = pushOutOfObstacle(curX, curZ, obs, entityRadius)
            if (hit) {
                curX = px
                curZ = pz
            }
        }

        // 2. Sub-stepped continuous resolution to prevent tunneling at high sprint speeds
        val deltaX = desiredX - curX
        val deltaZ = desiredZ - curZ
        val totalDist = hypot(deltaX, deltaZ)

        if (totalDist < 0.0001f) {
            val boundary = 48.0f
            return Pair(curX.coerceIn(-boundary, boundary), curZ.coerceIn(-boundary, boundary))
        }

        // Subdivide movement if moving fast (maximum step 0.10m)
        val steps = ceil(totalDist / 0.10f).toInt().coerceIn(1, 8)
        val stepDx = deltaX / steps
        val stepDz = deltaZ / steps

        for (s in 0 until steps) {
            var nextX = curX + stepDx
            var nextZ = curZ + stepDz

            // Iterative collision resolution with sliding response
            for (iter in 0 until 4) {
                var collisionOccurred = false
                for (obs in allObstacles) {
                    val (resolvedX, resolvedZ, hit) = resolveAndSlide(curX, curZ, nextX, nextZ, obs, entityRadius)
                    if (hit) {
                        nextX = resolvedX
                        nextZ = resolvedZ
                        collisionOccurred = true
                    }
                }
                if (!collisionOccurred) break
            }

            curX = nextX
            curZ = nextZ
        }

        // 3. Final safety penetration enforcement passes
        for (pass in 0 until 2) {
            for (obs in allObstacles) {
                val (px, pz, hit) = pushOutOfObstacle(curX, curZ, obs, entityRadius)
                if (hit) {
                    curX = px
                    curZ = pz
                }
            }
        }

        // 4. World perimeter boundary enforcement
        val boundary = 48.0f
        return Pair(curX.coerceIn(-boundary, boundary), curZ.coerceIn(-boundary, boundary))
    }

    /**
     * Resolves movement from (fromX, fromZ) to (toX, toZ) against an obstacle.
     * If an intersection occurs, pushes the entity to the obstacle boundary and slides along the surface.
     */
    private fun resolveAndSlide(
        fromX: Float,
        fromZ: Float,
        toX: Float,
        toZ: Float,
        obstacle: Obstacle,
        entityRadius: Float
    ): Triple<Float, Float, Boolean> {
        val (pushedX, pushedZ, isHit, normX, normZ) = getPenetrationAndNormal(toX, toZ, obstacle, entityRadius)
        if (!isHit) {
            return Triple(toX, toZ, false)
        }

        // Vector of attempted motion
        val vx = toX - fromX
        val vz = toZ - fromZ
        val dot = vx * normX + vz * normZ

        // If moving toward obstacle surface, project remaining motion tangent to contact normal (slide)
        val slideX = if (dot < 0f) vx - dot * normX else vx
        val slideZ = if (dot < 0f) vz - dot * normZ else vz

        val finalX = fromX + slideX
        val finalZ = fromZ + slideZ

        // Push firmly out to obstacle boundary along normal
        val (safeX, safeZ, _) = pushOutOfObstacle(finalX, finalZ, obstacle, entityRadius)
        return Triple(safeX, safeZ, true)
    }

    /**
     * Pushes point (px, pz) strictly outside obstacle if it is within entityRadius.
     */
    private fun pushOutOfObstacle(
        px: Float,
        pz: Float,
        obstacle: Obstacle,
        entityRadius: Float
    ): Triple<Float, Float, Boolean> {
        val (pushedX, pushedZ, hit, _, _) = getPenetrationAndNormal(px, pz, obstacle, entityRadius)
        return Triple(pushedX, pushedZ, hit)
    }

    /**
     * Returns: (resolvedX, resolvedZ, hasOverlap, normalX, normalZ)
     */
    private fun getPenetrationAndNormal(
        px: Float,
        pz: Float,
        obstacle: Obstacle,
        entityRadius: Float
    ): PenetrationResult {
        return when (obstacle) {
            is Obstacle.Circle -> {
                val dx = px - obstacle.x
                val dz = pz - obstacle.z
                val distSq = dx * dx + dz * dz
                val minDist = obstacle.radius + entityRadius + 0.015f

                if (distSq < minDist * minDist) {
                    val dist = sqrt(distSq)
                    val nx = if (dist > 0.0001f) dx / dist else 1f
                    val nz = if (dist > 0.0001f) dz / dist else 0f
                    PenetrationResult(
                        obstacle.x + nx * minDist,
                        obstacle.z + nz * minDist,
                        true,
                        nx,
                        nz
                    )
                } else {
                    PenetrationResult(px, pz, false, 0f, 0f)
                }
            }

            is Obstacle.Box -> {
                val cosR = cos(-obstacle.rotationY)
                val sinR = sin(-obstacle.rotationY)
                val dx = px - obstacle.x
                val dz = pz - obstacle.z
                val localX = dx * cosR - dz * sinR
                val localZ = dx * sinR + dz * cosR

                val halfW = obstacle.halfWidth
                val halfL = obstacle.halfLength

                val clampedX = localX.coerceIn(-halfW, halfW)
                val clampedZ = localZ.coerceIn(-halfL, halfL)

                val diffX = localX - clampedX
                val diffZ = localZ - clampedZ
                val distSq = diffX * diffX + diffZ * diffZ
                val margin = entityRadius + 0.015f

                val cosW = cos(obstacle.rotationY)
                val sinW = sin(obstacle.rotationY)

                if (distSq < 0.000001f) {
                    // Center is strictly inside box volume: push outward to nearest face
                    val penL = localX - (-halfW)
                    val penR = halfW - localX
                    val penB = localZ - (-halfL)
                    val penF = halfL - localZ
                    val minPen = minOf(penL, penR, penB, penF)

                    val (lnx, lnz) = when (minPen) {
                        penL -> Pair(-1f, 0f)
                        penR -> Pair(1f, 0f)
                        penB -> Pair(0f, -1f)
                        else -> Pair(0f, 1f)
                    }

                    val newLocalX = when (minPen) {
                        penL -> -halfW - margin
                        penR -> halfW + margin
                        else -> localX
                    }
                    val newLocalZ = when (minPen) {
                        penB -> -halfL - margin
                        penF -> halfL + margin
                        else -> localZ
                    }

                    val worldNx = lnx * cosW - lnz * sinW
                    val worldNz = lnx * sinW + lnz * cosW
                    val resX = obstacle.x + newLocalX * cosW - newLocalZ * sinW
                    val resZ = obstacle.z + newLocalX * sinW + newLocalZ * cosW
                    PenetrationResult(resX, resZ, true, worldNx, worldNz)
                } else if (distSq < margin * margin) {
                    // Center is outside box but within margin radius
                    val dist = sqrt(distSq)
                    val lnx = diffX / dist
                    val lnz = diffZ / dist

                    val newLocalX = clampedX + lnx * margin
                    val newLocalZ = clampedZ + lnz * margin

                    val worldNx = lnx * cosW - lnz * sinW
                    val worldNz = lnx * sinW + lnz * cosW
                    val resX = obstacle.x + newLocalX * cosW - newLocalZ * sinW
                    val resZ = obstacle.z + newLocalX * sinW + newLocalZ * cosW
                    PenetrationResult(resX, resZ, true, worldNx, worldNz)
                } else {
                    PenetrationResult(px, pz, false, 0f, 0f)
                }
            }
        }
    }

    private data class PenetrationResult(
        val resolvedX: Float,
        val resolvedZ: Float,
        val hit: Boolean,
        val normX: Float,
        val normZ: Float
    )
}
