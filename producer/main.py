#! /usr/bin/env python3

import logging
import random
import json
import time

import hazelcast
from kafka import KafkaProducer
logging.basicConfig(level="DEBUG")


def serialize_int(n: int) -> bytes:
    return n.to_bytes(length=8, byteorder="big", signed=True)

def main():
    topic = "orders"
    client = hazelcast.HazelcastClient()
    m = client.get_map("desserts").blocking()
    dessert_count = m.size()
    client.shutdown()
    if dessert_count == 0:
        raise Exception("There are no desserts!")
    producer = KafkaProducer(
        key_serializer=serialize_int,
        value_serializer=str.encode,
    )
    key = int(time.time() * 10)
    for i in range(10000):
        value = json.dumps({
            "dessertId": random.randint(0, dessert_count - 1),
            "itemCount": random.randint(1, 6),
        })
        producer.send(topic, value=value, key=key + i)
        time.sleep(1)
    producer.close(timeout=10)

if __name__ == "__main__":
    main()
