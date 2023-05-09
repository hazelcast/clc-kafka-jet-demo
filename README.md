# Hazelcast Jet with Kafka source Demo

## Requirements

* JDK 8 or better
* Hazelcast 5.3.0-BETA-2 or better: https://hazelcast.com/open-source-projects/downloads/
* Hazelcast CLC v5.3.0-BETA-2 or better: https://github.com/hazelcast/hazelcast-commandline-client/releases
* A recent version of Apache Kafka: https://kafka.apache.org/downloads


## Steps for demo

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

- Add cluster configuration for CLC 
``` bash
# This command will use the default values so it will connect to localhost:5701
clc config add
```

- Create the initial data and SQL mappings
``` bash
cat dessert.sql | clc -c local
```

- Build jar by running `make build`

- Create the pipeline by submitting the `orders` job to the cluster
``` bash
clc -c local job submit build/libs/jet-pipeline-sample-1.0-SNAPSHOT-all.jar --name orders
```

- Start the Kafka topic producer
``` bash
cd producer
pip3 install -r requirements.txt
python3 main.py
```

- See the `orders` map grow
``` bash
while true; do clc -c local map size -n orders; sleep 1; done
```

- Suspend the `orders` Job
``` bash
# See the job is in running state
clc -c local job list
# Suspend the job
clc -c local job suspend orders
# See the job is in suspended state
clc -c local job list
```

- See the `orders` map stay the same in size
``` bash
while true; do clc -c local map size -n orders; sleep 1; done
```

- Resume the `orders` Job
``` bash
clc -c local job resume orders
# See the job is in running state
clc -c local job list
```

- See the `orders` map grow
``` bash
while true; do clc -c local map size -n orders; sleep 1; done
```

- Export the Snapshot of the `orders` job (Enterprise and Viridian only)
``` bash
clc -c local export-snapshot orders --name snapshot1
# See the snapshot created
clc -c local snapshot list
```

- Cancel the running `orders` job
``` bash
clc -c local job cancel orders
# See the job is cancelled
clc -c local job list
```

- Create a new `orders2` job from the snapshot
``` bash
clc -c local job submit build/libs/jet-pipeline-sample-1.0-SNAPSHOT-all.jar --snapshot snapshot1 --name orders2
# See the new job is running
clc -c local job list
```

- See the `orders` map grow
``` bash
while true; do clc -c local map size -n orders; sleep 1; done
```

- Delete the snapshot
``` bash
clc -c local snapshot delete snapshot1
# See the snapshot is deleted
clc -c local snapshot list
```


### Clean up

- Stop the produces by pressing `Ctrl+C`

- Stop Kafka
``` bash
bin/kafka-server-stop.sh
```

- Stop Zookeper
``` bash
bin/zookeper-server-stop.sh
```

- If you want, you can clean up the Kafka and Zookeper logs by checking the log locations in their respective properties files