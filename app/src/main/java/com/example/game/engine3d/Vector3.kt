package com.example.game.engine3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vector3 = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else this
    operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 0.00001f) this / len else Vector3(0f, 0f, 0f)
    }

    fun distanceTo(other: Vector3): Float = (this - other).length()

    fun rotateY(angleRad: Float): Vector3 {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3(
            x = x * cosA + z * sinA,
            y = y,
            z = -x * sinA + z * cosA
        )
    }

    fun rotateX(angleRad: Float): Vector3 {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3(
            x = x,
            y = y * cosA - z * sinA,
            z = y * sinA + z * cosA
        )
    }

    fun rotateZ(angleRad: Float): Vector3 {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3(
            x = x * cosA - y * sinA,
            y = x * sinA + y * cosA,
            z = z
        )
    }

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, 1f)
        val RIGHT = Vector3(1f, 0f, 0f)

        fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
            val clampedT = t.coerceIn(0f, 1f)
            return a + (b - a) * clampedT
        }
    }
}
