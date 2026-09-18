package dev.kmpapp.data.remote.network.adapters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * This adapter is used to convert color hex field into [Long] field by marking them with [SerialName]
 */
object ColorHexJsonAdapter : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ColorHex", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long =
        decoder
            .decodeString()
            .removePrefix("#")
            .let {
                when (it.length) {
                    6 -> "ff$it"
                    8 -> it
                    else -> null
                }
            }?.toLong(16) ?: 0

    override fun serialize(
        encoder: Encoder,
        value: Long,
    ) {
        encoder.encodeString("#" + value.toString(16).padStart(8, '0'))
    }
}
