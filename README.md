# Hazelcast Jet with Kafka source Demo

## Requirements

* JDK 8 or better
* Hazelcast 5.3.0-BETA-2 or better: https://hazelcast.com/open-source-projects/downloads/
* Hazelcast CLC v5.3.0-BETA-2 or better: https://github.com/hazelcast/hazelcast-commandline-client/releases
* A recent version of Apache Kafka: https://kafka.apache.org/downloads

## Usage

### Building the Project

```
make
```

### Steps for demo

- Run Hazelcast locally
``` bash
# Change directory into extracted Hazelcast Distribution folder and Run Hazelcast
bin/hz-start
``` 
- Run Kafka broker locally 
``` bash
# Change directory into extracted Kafka folder
# Run Zookeper and Kafka Broker in separate tabs
bin/zookeper-server-start.sh config/zookeper.properties
bin/kafka-server-start.sh config/server.properties
``` 

- Build jar by running `make build`

- Add cluster configuration for CLC 
``` bash
clc config add local cluster.name=dev cluster.address=localhost:5701
```

- Create mappings for SQL
``` bash
cat dessert.sql | clc -c local
```

- Submit Jet Job JAR
``` bash
clc -c local job submit build/libs/jet-pipeline-sample-1.0-SNAPSHOT-all.jar --name orders
```

- Start producer
``` bash
cd producer
pip3 install -r requirements.txt
python3 main.py
```

- See `orders` map grow
``` bash
while true; do clc -c local map size -n orders; sleep 1; done
```

### Clean up

- Stop the produces by stroking `Ctrl+C`

- Stop Kafka
``` bash
kafka/bin/kafka-server-stop.sh
```

- Stop Zookeper
``` bash
kafka/bin/zookeper-server-stop.sh
```

- If you want, you can clean up the Kafka and Zookeper logs by checking the log locations in their respective properties files