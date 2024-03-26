package dev.nohus.rift.network.dscaninfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DscanInfoScanResponse(
    @SerialName("ships")
    val ships: List<Ship>? = null,
)

@Serializable
data class Ship(
    @SerialName("count")
    val count: Int,
    @SerialName("name")
    val name: String,
)
