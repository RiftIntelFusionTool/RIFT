package dev.nohus.rift.map

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.utils.roundSecurity

object SecurityColors {

    operator fun get(security: Double): Color {
        val value = security.roundSecurity()
        return when {
            value >= 1.0 -> Color(0xFF2E74DF)
            value >= 0.9 -> Color(0xFF379CF6)
            value >= 0.8 -> Color(0xFF4ACFF3)
            value >= 0.7 -> Color(0xFF5CDCA6)
            value >= 0.6 -> Color(0xFF70E552)
            value >= 0.5 -> Color(0xFFEEFF83)
            value >= 0.4 -> Color(0xFFDC6C08)
            value >= 0.3 -> Color(0xFFCE4611)
            value >= 0.2 -> Color(0xFFBC1113)
            value >= 0.1 -> Color(0xFF6D231A)
            else -> Color(0xFF8F3068)
        }
    }
}
