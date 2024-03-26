package dev.nohus.rift.compose

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.nohus.rift.di.koin
import dev.nohus.rift.repositories.StarGatesRepository

@Composable
fun GateIcon(
    isAnsiblex: Boolean,
    fromSystem: String?,
    toSystem: String,
    size: Dp,
) {
    val gatesRepository: StarGatesRepository by koin.inject()
    val typeId = if (isAnsiblex) {
        35841
    } else {
        val gateTypeId = if (fromSystem != null) {
            gatesRepository.getStargateTypeId(fromSystem, toSystem)
        } else {
            null
        }
        gateTypeId ?: 16 // Default to Caldari system gate
    }
    AsyncTypeIcon(
        typeId = typeId,
        modifier = Modifier.size(size),
    )
}
