package dev.nohus.rift.planetaryindustry.models

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class SurfacePoint(
    private val radius: Float,
    private val theta: Float,
    private val phi: Float,
) {
    val x: Float
    val y: Float
    val z: Float

    init {
        val radSinPhi = radius * sin(phi)
        x = radSinPhi * cos(theta)
        z = radSinPhi * sin(theta)
        y = radius * cos(phi)
    }

    fun getDistanceTo(other: SurfacePoint): Float {
        return radius * getAngleTo(other)
    }

    private fun getAngleTo(other: SurfacePoint): Float {
        var dotProduct = (x * other.x + y * other.y + z * other.z) / radius / other.radius
        if (dotProduct > 1f) dotProduct = 1f
        return acos(dotProduct)
    }
}
