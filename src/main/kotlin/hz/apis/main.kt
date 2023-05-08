package hz.apis

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.HazelcastJsonValue
import com.hazelcast.jet.aggregate.AggregateOperation
import com.hazelcast.jet.datamodel.Tuple2
import com.hazelcast.jet.datamodel.Tuple2.tuple2
import com.hazelcast.jet.json.JsonUtil
import com.hazelcast.jet.kafka.KafkaSources
import com.hazelcast.jet.pipeline.Pipeline
import com.hazelcast.jet.pipeline.ServiceFactories.nonSharedService
import com.hazelcast.jet.pipeline.ServiceFactory
import com.hazelcast.jet.pipeline.Sinks
import com.hazelcast.sql.SqlRow
import com.hazelcast.sql.SqlService
import com.hazelcast.sql.impl.row.Row
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*


fun main() {
    val props = Properties().apply {
        setProperty("bootstrap.servers", "localhost:9092")
        setProperty("key.deserializer", StringDeserializer::class.java.canonicalName)
        setProperty("value.deserializer", StringDeserializer::class.java.canonicalName)
        setProperty("auto.offset.reset", "earliest")
        setProperty("group.id", "order-consumers")
        setProperty("auto.commit.interval.ms","1000")
    }
    val hz = Hazelcast.bootstrappedInstance()
    val serviceFactory: ServiceFactory<*, HazelcastInstance> =
        nonSharedService{ println("CREATING SERVICE FACTORY"); Hazelcast.bootstrappedInstance() }

    val pipeline = Pipeline.create()
    pipeline.readFrom(KafkaSources.kafka<String, String>(props, "orders"))
        .withNativeTimestamps(0)
        .setName("copy from Kafka orders to Map orders")
        .mapUsingIMap<Int,HazelcastJsonValue,Tuple2<Map.Entry<String,String>,HazelcastJsonValue>>("desserts",
            { order -> getDessertID(order.value) },
            { order, des -> tuple2(order,des)} )
        .mapUsingService(serviceFactory) { factory, orderTuple -> createOrder(orderTuple, factory.getSql()) }
        // .map { orderTuple -> createOrder(orderTuple,hz.getSql()) }
        // .groupingKey { tuple -> getDessertCategory( tuple.f1()!!) }
        // .window(WindowDefinition.tumbling(TimeUnit.SECONDS.toMillis(5)))
        // .aggregate(aggrOp)
        .writeTo(Sinks.logger())
        // .writeTo(Sinks.map(
        //     "orders",
        //     { it.key },
        //     { it.value },
        // ))
    hz.jet.newJob(pipeline)
}

fun getDessertCategory(value: HazelcastJsonValue) : String {
    return JsonUtil.mapFrom(value.toString())?.get("category").toString()
}

fun createOrder(orderDessertTuple: Tuple2<Map.Entry<String,String>,HazelcastJsonValue>, sqlService: SqlService): HazelcastJsonValue {
    println(orderDessertTuple)
     try {
        var category = JsonUtil.mapFrom(orderDessertTuple.f1()!!.toString())?.get("category").toString()

        var result = sqlService.execute("SELECT * FROM desserts WHERE category = '$category'")
         println(result.isRowSet)
        var it =  result.iterator()
        var myArrayList = ArrayList<SqlRow>()
        while (it.hasNext()) {
             myArrayList.add(it.next())
        }
//        val mylist = myArrayList.map { row -> row.getObject<String>("name") }
//         println(mylist)

        //result.forEach { println(it) }
//         val recommendations = result.shuffled()
//            .subList(0,3)
//            .map { row -> row.getObject<String>("name")}
//         result.close()

//         val orderItem = JsonUtil.mapFrom(orderDessertTuple.f0()!!.value)!!
//         return JsonUtil.hazelcastJsonValue(orderItem)
//
//         orderItem["recommendations"] = JsonUtil.hazelcastJsonValue(recommendations)
         return HazelcastJsonValue("""" {"result" : "pass" }""")

     } catch (e: Exception) {
        println(e)
         return HazelcastJsonValue("""" {"result" : "fail" }""")
    }
}


fun getDessertID(orderVal: String): Int {
    return try {
        val m = JsonUtil.mapFrom(orderVal)!!
        m["dessertID"].toString().toInt()
    } catch (e: Exception) {
        0
    }
}
var aggrOp = AggregateOperation
    .withCreate { CounterAggregator() }
    .andAccumulate { acc: CounterAggregator, n: Tuple2<Map.Entry<String,String>,HazelcastJsonValue> ->
        acc.accumulate(n)
    }
    .andCombine { left: CounterAggregator, right: CounterAggregator ->
        left.combine(right)
    }
    .andDeduct { left: CounterAggregator, right: CounterAggregator ->
        left.deduct(right)
    }
    .andExportFinish { acc: CounterAggregator ->
        acc.finish()
    }
