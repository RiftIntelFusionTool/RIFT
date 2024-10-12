package dev.nohus.rift.map

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.utils.roundSecurity

object SecurityColors {

    operator fun get(security: Double): Color {
        val value = security.roundSecurity()
        return when {
            value >= 1.0 -> Color(0xFF2C75E1)
            value >= 0.9 -> Color(0xFF399AEB)
            value >= 0.8 -> Color(0xFF4ECEF8)
            value >= 0.7 -> Color(0xFF60DBA3)
            value >= 0.6 -> Color(0xFF71E754)
            value >= 0.5 -> Color(0xFFF5FF83)
            value >= 0.4 -> Color(0xFFDC6C06)
            value >= 0.3 -> Color(0xFFCE440F)
            value >= 0.2 -> Color(0xFFBB1116)
            value >= 0.1 -> Color(0xFF731F1F)
            else -> Color(0xFF8D3163)
        }
    }
}
