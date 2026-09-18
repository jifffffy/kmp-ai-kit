package dev.kmpapp.data.remote.network.adapters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * This adapter is used to add + into phone-numbers by marking them with [PhoneNumber]
 */
object PhoneJsonAdapter : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PhoneNumber", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val rawPhone = decoder.decodeString()
        return if (rawPhone.isNotBlank() && rawPhone.startsWith("+").not()) {
            "+$rawPhone"
        } else {
            rawPhone
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: String,
    ) = encoder.encodeString(value)
}
