package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine3d.Matrix4
import com.example.game.engine3d.Vector3
import com.example.game.model.CharacterRole
import com.example.game.viewmodel.GameScreenState
import com.example.game.viewmodel.GameViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Outpost: Mission Survival", appName)
    }

    @Test
    fun `vector3 math operations`() {
        val v1 = Vector3(1f, 2f, 3f)
        val v2 = Vector3(4f, 5f, 6f)
        val sum = v1 + v2
        assertEquals(5f, sum.x, 0.001f)
        assertEquals(7f, sum.y, 0.001f)
        assertEquals(9f, sum.z, 0.001f)

        val dist = Vector3(0f, 0f, 0f).distanceTo(Vector3(3f, 4f, 0f))
        assertEquals(5f, dist, 0.001f)
    }

    @Test
    fun `matrix4 translation and lookAt`() {
        val mat = Matrix4.translation(10f, 20f, 30f)
        val v = Vector3(1f, 2f, 3f)
        val transformed = mat.transform(v)
        assertEquals(11f, transformed.x, 0.001f)
        assertEquals(22f, transformed.y, 0.001f)
        assertEquals(33f, transformed.z, 0.001f)
    }

    @Test
    fun `gameViewModel initializes with Chapter 1 and nominal resources`() {
        val vm = GameViewModel()
        val state = vm.uiState.value

        assertEquals(GameScreenState.MAIN_MENU, state.currentScreen)
        assertNotNull(state.currentMission)
        assertEquals("Chapter 1: First Landing", state.currentMission?.title)
        assertTrue(state.outpostStats.powerKw > 0)
        assertTrue(state.outpostStats.oxygenLevelPercent > 80f)
        assertEquals(5, vm.getNpcCharacters().size)
        assertTrue(vm.getInteractiveObjects().isNotEmpty())
    }

    @Test
    fun `gameViewModel character role selection and scanner toggle`() {
        val vm = GameViewModel()
        val profile = vm.uiState.value.playerProfile.copy(
            name = "Commander Shepard",
            role = CharacterRole.ENGINEER
        )
        vm.updateCharacterProfile(profile)
        assertEquals("Commander Shepard", vm.uiState.value.playerProfile.name)
        assertEquals(CharacterRole.ENGINEER, vm.uiState.value.playerProfile.role)

        assertFalse(vm.uiState.value.scannerActive)
        vm.toggleScanner()
        assertTrue(vm.uiState.value.scannerActive)
    }

    @Test
    fun `gameViewModel environmental storm simulation and clearance`() {
        val vm = GameViewModel()
        assertFalse(vm.uiState.value.environmentalEvent.isEmergencyAlert)

        vm.triggerEnvironmentalStorm()
        assertTrue(vm.uiState.value.environmentalEvent.isEmergencyAlert)

        vm.clearEnvironmentalHazard()
        assertFalse(vm.uiState.value.environmentalEvent.isEmergencyAlert)
    }

    @Test
    fun `gameViewModel celestial body switch`() {
        val vm = GameViewModel()
        assertFalse(vm.uiState.value.isMars) // Defaults to Moon

        vm.switchCelestialBody(true)
        assertTrue(vm.uiState.value.isMars)
    }

    @Test
    fun `all scene meshes have valid vertex indices`() {
        val vm = GameViewModel()
        val meshes = vm.getSceneMeshes()
        assertTrue(meshes.isNotEmpty())
        for (mesh in meshes) {
            val vCount = mesh.vertices.size
            for (face in mesh.faces) {
                assertTrue("face.v0 < vCount in ${mesh.name}", face.v0 in 0 until vCount)
                assertTrue("face.v1 < vCount in ${mesh.name}", face.v1 in 0 until vCount)
                assertTrue("face.v2 < vCount in ${mesh.name}", face.v2 in 0 until vCount)
                if (face.v3 >= 0) {
                    assertTrue("face.v3 < vCount in ${mesh.name}", face.v3 in 0 until vCount)
                }
            }
        }
    }

    @Test
    fun `test scene meshes stats`() {
        val vm = GameViewModel()
        val meshes = vm.getSceneMeshes()
        var totalFaces = 0
        var totalVerts = 0
        for (m in meshes) {
            totalFaces += m.faces.size
            totalVerts += m.vertices.size
        }
        println("TOTAL MESHES: ${meshes.size}, TOTAL VERTS: $totalVerts, TOTAL FACES: $totalFaces")
        assertTrue(totalFaces > 0)
    }

    @Test
    fun `test collision prevents passing through habitat dome`() {
        val (resX, resZ) = com.example.game.engine3d.CollisionSystem.resolvePosition(
            startX = 0f,
            startZ = 8.5f,
            desiredX = 0f,
            desiredZ = 2.0f, // Tries to walk straight into dome center
            entityRadius = 0.45f
        )
        // Must be stopped by dome or airlock
        val distToCenter = kotlin.math.hypot(resX, resZ)
        assertTrue("Character must not penetrate dome: dist = $distToCenter", distToCenter >= 5.65f)
        assertTrue("Character must not penetrate airlock door: resZ = $resZ", resZ >= 8.25f)
    }

    @Test
    fun `test collision prevents passing through boulder`() {
        val (resX, resZ) = com.example.game.engine3d.CollisionSystem.resolvePosition(
            startX = 18f,
            startZ = 22f,
            desiredX = 18f,
            desiredZ = 19f, // Boulder center at 18, 19
            entityRadius = 0.45f
        )
        val distToBoulder = kotlin.math.hypot(resX - 18f, resZ - 19f)
        assertTrue("Character must not penetrate boulder: dist = $distToBoulder", distToBoulder >= 2.10f)
    }

    @Test
    fun `test collision sliding along wall without tunneling`() {
        // Move towards airlock wall diagonally
        val (resX, resZ) = com.example.game.engine3d.CollisionSystem.resolvePosition(
            startX = -3.0f,
            startZ = 6.7f,
            desiredX = 0f, // Towards center of airlock
            desiredZ = 7.5f, // Moving forward along wall
            entityRadius = 0.45f
        )
        // Must slide along wall, not penetrate
        assertTrue("resX must remain outside airlock wall: resX = $resX", resX <= -1.25f - 0.45f + 0.05f)
        assertTrue("resZ should have progressed along wall: resZ = $resZ", resZ > 6.7f)
    }
}
