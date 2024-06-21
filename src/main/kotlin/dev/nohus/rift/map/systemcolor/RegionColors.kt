package dev.nohus.rift.map.systemcolor

import androidx.compose.ui.graphics.Color

object RegionColors {

    fun getColor(region: String): Color {
        val caldari = listOf("Lonetrek", "Black Rise", "The Citadel", "The Forge")
        val gallente = listOf("Placid", "Essence", "Verge Vendor", "Everyshore", "Sinq Laison", "Solitude")
        val amarr = listOf("Aridia", "Khanid", "Kor-Azor", "Kador", "Tash-Murkon", "Domain", "Devoid", "The Bleak Lands")
        val minmatar = listOf("Derelik", "Heimatar", "Molden Heath", "Metropolis")
        return when (region) {
            in caldari -> Color(0xFF9AD2E3)
            in gallente -> Color(0xFF6DB09E)
            in amarr -> Color(0xFFFFEE93)
            in minmatar -> Color(0xFF9D452D)
            "Pochven" -> Color(0xFFBC1113)
            "Yasna Zakh" -> Color.White
            else -> Color(0xFF8F3068)
        }
    }
}
