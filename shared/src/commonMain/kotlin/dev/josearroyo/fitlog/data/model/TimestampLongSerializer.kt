package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import dev.gitlive.firebase.firestore.Timestamp

object TimestampLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TimestampLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        return try {
            decoder.decodeLong()
        } catch (e: Exception) {
            try {
                val timestamp = decoder.decodeSerializableValue(Timestamp.serializer())
                timestamp.seconds * 1000L + (timestamp.nanoseconds / 1_000_000)
            } catch (nestedEx: Exception) {
                0L
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        try {
            val seconds = value / 1000L
            val nanoseconds = ((value % 1000L) * 1_000_000L).toInt()
            encoder.encodeSerializableValue(Timestamp.serializer(), Timestamp(seconds, nanoseconds))
        } catch (e: Exception) {
            encoder.encodeLong(value)
        }
    }
}