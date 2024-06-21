package dev.nohus.rift.assets

import dev.nohus.rift.assets.AssetsViewModel.Asset
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.utils.toURIOrNull
import org.koin.core.annotation.Single
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.URI
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Single
class FittingController(
    private val typesRepository: TypesRepository,
) {

    data class Fitting(
        val eft: String,
        val eftWithoutCargo: String,
    )

    /**
     * Fills the EFT fitting field on asset if it contains a fit
     */
    fun fillFitting(asset: Asset): Asset {
        val fitting = getEftFitting(asset)
        return if (fitting != null) {
            asset.copy(fitting = fitting)
        } else {
            asset
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun getEveShipFitUri(eft: String): URI? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val writer = BufferedWriter(OutputStreamWriter(GZIPOutputStream(byteArrayOutputStream)))
        writer.append(eft)
        writer.close()
        val base64 = Base64.Default.encode(byteArrayOutputStream.toByteArray())
        return "https://eveship.fit/?fit=eft:$base64".toURIOrNull()
    }

    private fun getEftFitting(asset: Asset): Fitting? {
        if (asset.children.isEmpty()) return null
        val typeName = typesRepository.getTypeName(asset.asset.typeId)
        val eftWithoutCargo = buildString {
            List(8) { "LoSlot$it" }.let { flags ->
                if (hasAnyFlag(asset, flags)) {
                    flags.forEach { addSlot(asset, it) }
                }
            }
            List(8) { "MedSlot$it" }.let { flags ->
                if (hasAnyFlag(asset, flags)) {
                    appendLine()
                    flags.forEach { addSlot(asset, it) }
                }
            }
            List(8) { "HiSlot$it" }.let { flags ->
                if (hasAnyFlag(asset, flags)) {
                    appendLine()
                    flags.forEach { addSlot(asset, it) }
                }
            }
            List(8) { "ServiceSlot$it" }.let { flags ->
                if (hasAnyFlag(asset, flags)) {
                    appendLine()
                    flags.forEach { addSlot(asset, it) }
                }
            }
            List(8) { "RigSlot$it" }.let { flags ->
                if (hasAnyFlag(asset, flags)) {
                    appendLine()
                    flags.forEach { addSlot(asset, it) }
                }
            }
            List(8) { "SubSystemSlot$it" }.let { flags ->
                if (hasAnyFlag(asset, flags)) {
                    appendLine()
                    flags.forEach { addSlot(asset, it) }
                }
            }
            addQuantifiedContents(
                asset,
                listOf(
                    "DroneBay",
                    "FighterBay",
                    "FighterTube0",
                    "FighterTube1",
                    "FighterTube2",
                    "FighterTube3",
                    "FighterTube4",
                ),
            )
        }
        val eftWithCargo = buildString {
            append(eftWithoutCargo)
            addQuantifiedContents(
                asset,
                listOf(
                    "Cargo",
                    "SubSystemBay",
                ),
                newLines = 2,
            )
        }
        if (eftWithoutCargo.isNotBlank()) {
            val fitTitle = "[${listOfNotNull(typeName, asset.name).joinToString(", ")}]"
            return Fitting(
                eft = "$fitTitle\n$eftWithCargo",
                eftWithoutCargo = "$fitTitle\n$eftWithoutCargo",
            )
        } else {
            return null
        }
    }

    private fun StringBuilder.addSlot(asset: Asset, locationFlag: String) {
        val item = asset.children.firstOrNull { it.asset.locationFlag == locationFlag } ?: return
        val name = typesRepository.getTypeName(item.asset.typeId) ?: return
        appendLine(name)
    }

    private fun StringBuilder.addQuantifiedContents(
        asset: Asset,
        locationFlags: List<String>,
        newLines: Int = 1,
    ) {
        val items = asset.children.filter { it.asset.locationFlag in locationFlags }
        if (items.isNotEmpty()) {
            repeat(newLines) {
                appendLine()
            }
            items.groupBy { it.asset.typeId }.forEach { (typeId, items) ->
                val name = typesRepository.getTypeName(typeId) ?: "$typeId"
                val quantity = items.sumOf { it.asset.quantity }
                appendLine("$name x$quantity")
            }
        }
    }

    private fun hasAnyFlag(asset: Asset, flags: List<String>): Boolean {
        return flags.any { flag -> asset.children.any { it.asset.locationFlag == flag } }
    }
}
