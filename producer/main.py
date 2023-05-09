#! /usr/bin/env python3

import logging
import random
import json
import time

import hazelcast
from kafka import KafkaProducer
logging.basicConfig(level="DEBUG")

topic = "orders"

client = hazelcast.HazelcastClient()
m = client.get_map("desserts").blocking()
print(m)

dessert_count = m.size()
if dessert_count == 0:
    raise Exception("There are no desserts!")

producer = KafkaProducer(key_serializer=str.encode, value_serializer=str.encode)
key = int(time.time() * 10)
for i in range(10, 20):
    value = json.dumps({
        "dessert_id": random.randint(0, dessert_count - 1),
        "count": random.randint(1, 6),
    })
    producer.send(topic, value=value, key=str(key + i))

producer.close(timeout=10)
client.shutdown()
