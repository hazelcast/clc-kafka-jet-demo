package hz.apis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastJsonValue
import com.hazelcast.jet.kafka.KafkaSources
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.Sinks
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

val mapper = jacksonObjectMapper()

fun main() {
    val props = Properties().apply {
        setProperty("bootstrap.servers", "localhost:9092")
        setProperty("key.deserializer", LongDeserializer::class.java.canonicalName)
        setProperty("value.deserializer", StringDeserializer::class.java.canonicalName)
        setProperty("group.id","dessert-order")
        setProperty("auto.offset.reset", "earliest")
    }
    val pipeline = Pipeline.create()
    pipeline.readFrom(KafkaSources.kafka<Long, String>(props, "orders"))
        .withoutTimestamps()
        .setName("read order from Kafka topic and enrich it using a map")
        .mapUsingIMap("desserts",
            // lookup value
            { event ->
                getDessertID(event.value) },
            // map and return new value
            { event, dessertJson: HazelcastJsonValue ->
                getEnrichedOrder(event, dessertJson) }
        )
        .setName("save enriched order to map")
        .writeTo(Sinks.map(
             "orders",
            // map key
             { it.first },
            // map value
             { it.second },
         ))
    val hz = Hazelcast.bootstrappedInstance()
    hz.jet.newJob(pipeline)
}

fun getDessertID(orderVal: String) =
    mapper.readValue<Order>(orderVal).dessertId

fun getEnrichedOrder(event: Map.Entry<Long, String>, dessertJson: HazelcastJsonValue): Pair<Long, HazelcastJsonValue> {
    val order = mapper.readValue<Order>(event.value)
    val dessert = mapper.readValue<Dessert>(dessertJson.toString())
    val enrichedOrder = EnrichedOrder(
        dessertId = order.dessertId,
        itemCount = order.itemCount,
        dessertName = dessert.name,
        dessertCategory = dessert.category,
    )
    val value = mapper.writeValueAsString(enrichedOrder)
    return event.key to HazelcastJsonValue(value)
}
