package hz.apis

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastJsonValue
import com.hazelcast.jet.json.JsonUtil
import com.hazelcast.jet.kafka.KafkaSources
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.ServiceFactories.nonSharedService
import com.hazelcast.jet.pipeline.Sinks
import com.hazelcast.sql.SqlService
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


fun main() {
    val props = Properties().apply {
        setProperty("bootstrap.servers", "localhost:9092")
        setProperty("key.deserializer", StringDeserializer::class.java.canonicalName)
        setProperty("value.deserializer", StringDeserializer::class.java.canonicalName)
    }
    val pipeline = Pipeline.create()
    pipeline.readFrom(KafkaSources.kafka<String, String>(props, "orders"))
        .withoutTimestamps()
        .setName("copy from Kafka orders to Map orders")
        .mapUsingIMap("desserts",
            { event -> getDessertID(event.value) },
            { event, dessertJson: HazelcastJsonValue ->
                val order = JsonUtil.mapFrom(event.value.toString())!!
                val dessert = JsonUtil.mapFrom(dessertJson.toString())!!
                val enrichedOrder = order.toMutableMap().apply {
                    putAll(dessert)
                }
                val value = JsonUtil.hazelcastJsonValue(JsonUtil.toJson(enrichedOrder))
                event.key to value
            }
        )
         .writeTo(Sinks.map(
             "orders",
             { it.first },
             { it.second },
         ))
    val hz = Hazelcast.bootstrappedInstance()
    hz.jet.newJob(pipeline)
}

fun getDessertID(orderVal: String): Int =
    JsonUtil.mapFrom(orderVal)?.get("dessert_id")?.toString()?.toInt() ?: 0
