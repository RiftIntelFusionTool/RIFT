package dev.nohus.rift.whatsnew

object VersionUtils {

    private const val SEGMENTS = 3

    fun isNewer(base: String, test: String): Boolean {
        val aParts = base.split(".")
        val bParts = test.split(".")
        if (aParts.size < SEGMENTS || bParts.size < SEGMENTS) return false
        repeat(SEGMENTS) {
            if (bParts[it].toInt() > aParts[it].toInt()) return true
            if (bParts[it].toInt() < aParts[it].toInt()) return false
        }
        return false
    }
}
