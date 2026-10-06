package com.example.game.engine3d

enum class SurfaceTexture {
    NONE,
    REGOLITH_DUST,
    SOLAR_CELLS,
    PAVER_TILES,
    METAL_PANELS,
    THERMAL_GOLD_FOIL,
    CRATER_ROCK,
    GREENHOUSE_GLASS,
    HAZARD_STRIPES
}

data class Face3D(
    val v0: Int,
    val v1: Int,
    val v2: Int,
    val v3: Int = -1, // Quad if >= 0, else triangle
    val baseColor: Long = 0xFFCCCCCC,
    val emissive: Boolean = false,
    val doubleSided: Boolean = true, // Always double-sided to prevent missing faces/blocks
    val specular: Float = 0.2f,
    val texture: SurfaceTexture = SurfaceTexture.NONE
)

data class Mesh3D(
    val vertices: List<Vector3>,
    val faces: List<Face3D>,
    var position: Vector3 = Vector3.ZERO,
    var rotation: Vector3 = Vector3.ZERO, // Euler angles in radians (x, y, z)
    var scale: Vector3 = Vector3(1f, 1f, 1f),
    val name: String = "Mesh"
)

data class ProjectedPolygon(
    val xCoords: FloatArray = FloatArray(8),
    val yCoords: FloatArray = FloatArray(8),
    var count: Int = 0,
    var averageDepth: Float = 0f,
    var finalColor: Int = 0,
    var isEmissive: Boolean = false,
    var texture: SurfaceTexture = SurfaceTexture.NONE
)

data class Particle3D(
    var position: Vector3,
    var velocity: Vector3,
    var life: Float,
    val maxLife: Float,
    val color: Long,
    val size: Float
)
