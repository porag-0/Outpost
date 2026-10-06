package com.example

import com.example.game.engine3d.CollisionSystem
import com.example.game.engine3d.MeshFactory
import com.example.game.engine3d.Vector3
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    private fun assertMeshValid(name: String, mesh: com.example.game.engine3d.Mesh3D) {
        val vCount = mesh.vertices.size
        for ((idx, face) in mesh.faces.withIndex()) {
            assertTrue("$name face $idx v0 ${face.v0} >= $vCount", face.v0 in 0 until vCount)
            assertTrue("$name face $idx v1 ${face.v1} >= $vCount", face.v1 in 0 until vCount)
            assertTrue("$name face $idx v2 ${face.v2} >= $vCount", face.v2 in 0 until vCount)
            if (face.v3 >= 0) {
                assertTrue("$name face $idx v3 ${face.v3} >= $vCount", face.v3 in 0 until vCount)
            }
        }
    }

    @Test
    fun testAllMeshesValid() {
        assertMeshValid("Terrain", MeshFactory.createTerrain())
        assertMeshValid("Dome", MeshFactory.createOutpostDome())
        assertMeshValid("Corridor", MeshFactory.createUtilityConduitTunnel())
        assertMeshValid("SolarArray", MeshFactory.createSolarArray())
        assertMeshValid("Greenhouse", MeshFactory.createGreenhouse())
        assertMeshValid("OxygenGen", MeshFactory.createOxygenGenerator())
        assertMeshValid("CommDish", MeshFactory.createCommDish())
        assertMeshValid("SpaceLander", MeshFactory.createSpaceLander())
        assertMeshValid("CryoStorage", MeshFactory.createCryoStorageUnit())
        assertMeshValid("ScienceRack", MeshFactory.createScienceFieldRack())
        assertMeshValid("BeaconPylon", MeshFactory.createPerimeterBeaconPylon(0xFF00E5FF))
        assertMeshValid("IceDeposit", MeshFactory.createMineralDeposit("ice"))
        assertMeshValid("Boulder", MeshFactory.createBoulder(Vector3(2f, 1.5f, 2f)))
        assertMeshValid("Astronaut", MeshFactory.createAstronautMesh())
        assertMeshValid("Rover", MeshFactory.createRoverMesh())
        assertMeshValid("Drone", MeshFactory.createDroneMesh())
    }

    @Test
    fun testCollisionSystem() {
        val (rx, rz) = CollisionSystem.resolvePosition(0f, 18f, 0f, 17f, 0.45f)
        assertFalse(rx.isNaN())
        assertFalse(rz.isNaN())
    }
}

