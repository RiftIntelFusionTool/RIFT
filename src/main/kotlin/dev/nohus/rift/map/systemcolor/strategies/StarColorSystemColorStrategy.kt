package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.koin.core.annotation.Single

@Single
class StarColorSystemColorStrategy(
    private val solarSystemsRepository: SolarSystemsRepository,
) : SystemColorStrategy() {

    private val activeColors = mutableMapOf<Int, Color>()

    override fun hasData(system: Int): Boolean {
        return true
    }

    override fun getColor(system: Int): Color {
        val sunTypeId = solarSystemsRepository.getSystem(system)?.sunTypeId ?: return Color.Unspecified
        return activeColors.getOrPut(sunTypeId) { getActualColor(sunTypeId) }
    }

    private fun getActualColor(sunTypeId: Int): Color {
        return when (sunTypeId) {
            6 -> Color(0xFFFDFD7F)
            7 -> Color(0xFFFDCB97)
            8 -> Color(0xFFFD9797)
            9 -> Color(0xFF97F1FB)
            10 -> Color(0xFFFDFDFB)
            3796 -> Color(0xFFCBF9FB)
            3797 -> Color(0xFFFDD7E3)
            3798 -> Color(0xFFFDBDA3)
            3799 -> Color(0xFFFDD7E3)
            3800 -> Color(0xFFFDCB97)
            3801 -> Color(0xFF97F1FB)
            3802 -> Color(0xFFFDFD7F)
            3803 -> Color(0xFFFDFDFB)
            34331 -> Color(0xFF752C78)
            45030 -> Color(0xFFF3F37A)
            45031 -> Color(0xFFFDCB97)
            45032 -> Color(0xFFF9C793)
            45033 -> Color(0xFFF79593)
            45034 -> Color(0xFF97F1FB)
            45035 -> Color(0xFFFBFBFB)
            45036 -> Color(0xFFFDD7E3)
            45037 -> Color(0xFFFDBDA3)
            45038 -> Color(0xFFFDD7E3)
            45039 -> Color(0xFFFDCB97)
            45040 -> Color(0xFFFDCB97)
            45041 -> Color(0xFFFBFB7E)
            45042 -> Color(0xFFFDFDFB)
            45046 -> Color(0xFF93EBF7)
            45047 -> Color(0xFFFBFB7E)
            56082 -> Color(0xFFCB0000)
            56083 -> Color(0xFFCB0000)
            56084 -> Color(0xFFC70000)
            56085 -> Color(0xFFC90000)
            56086 -> Color(0xFFC90000)
            56097 -> Color(0xFFCB0000)
            56098 -> Color(0xFFCB0000)
            73909 -> Color(0xFF97F1FB)
            // Zarzakh -> Color(0xFF752C78)
            else -> Color.White
        }
    }
}
