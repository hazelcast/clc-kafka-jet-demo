package hz.apis

import com.hazelcast.core.HazelcastJsonValue
import com.hazelcast.jet.datamodel.Tuple2
import com.hazelcast.jet.json.JsonUtil


class CounterAggregator {
    private var sum: Long = 0
    fun accumulate(value: Tuple2<Map.Entry<String, String>, HazelcastJsonValue>) {
        sum +=JsonUtil.mapFrom(value.f0()!!.value.toString())?.get("count").toString().toInt()

    }

    fun combine(that: CounterAggregator) {
        sum += that.sum
    }
    fun deduct(that: CounterAggregator) {
        sum -= that.sum
    }
    fun finish(): Double {
        return sum.toDouble()
    }
}
