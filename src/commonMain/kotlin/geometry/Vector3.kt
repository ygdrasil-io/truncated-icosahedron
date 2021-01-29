package geometry

import kotlin.math.sqrt

class Vector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {

    constructor(other: Vector3) : this(other.x, other.y, other.z)

    operator fun plus(other: Vector3): Vector3 {
        return Vector3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vector3): Vector3 {
        return Vector3(x - other.x, y - other.y, z - other.z)
    }

    operator fun div(value: Float): Vector3 = Vector3(x / value, y / value, z / value)

    operator fun times(scalar: Float): Vector3 {
        return Vector3(x * scalar, y * scalar, z * scalar)
    }

    fun normalize(): Vector3 {
        val len = x * x + y * y + z * z
        return if (len == 0f || len == 1f) Vector3(this) else times(1f / sqrt(len.toDouble()).toFloat())
    }

    fun dot(vector: Vector3): Float {
        return x * vector.x + y * vector.y + z * vector.z
    }

    fun cross(vector: Vector3): Vector3 {
        return Vector3(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)
    }

    fun lerp(target: Vector3, alpha: Float): Vector3 {
        return Vector3(
            x + alpha * (target.x - x),
            y + alpha * (target.y - y),
            z + alpha * (target.z - z)
        )
    }

}
