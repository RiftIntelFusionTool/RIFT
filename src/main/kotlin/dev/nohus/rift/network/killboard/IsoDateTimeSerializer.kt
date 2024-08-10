package dev.nohus.rift.network.killboard

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

object IsoDateTimeSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(dateFormatter.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.from(dateFormatter.parse(decoder.decodeString()))
    }
}
