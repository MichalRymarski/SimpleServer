@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package mr.server.formats

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.http4k.format.ConfigurableJackson
import org.http4k.format.standardConfig
import kotlin.time.Instant
import kotlin.uuid.Uuid

private object KotlinInstantModule : SimpleModule() {
    init {
        addSerializer(Instant::class.java, object : StdSerializer<Instant>(Instant::class.java) {
            override fun serialize(value: Instant, gen: JsonGenerator, provider: SerializerProvider) {
                gen.writeString(value.toString())
            }
        })
        addDeserializer(Instant::class.java, object : StdDeserializer<Instant>(Instant::class.java) {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant =
                Instant.parse(p.text)
        })
    }
}

object AppJson : ConfigurableJackson(
    standardConfig { this }
        .registerModule(KotlinInstantModule)
        .registerModule(KotlinUuidModule)
)

private object KotlinUuidModule : SimpleModule() {
    init {
        addSerializer(Uuid::class.java, object : StdSerializer<Uuid>(Uuid::class.java) {
            override fun serialize(value: Uuid, gen: JsonGenerator, provider: SerializerProvider) {
                gen.writeString(value.toString())
            }
        })
        addDeserializer(Uuid::class.java, object : StdDeserializer<Uuid>(Uuid::class.java) {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Uuid =
                Uuid.parse(p.text)
        })
    }
}
