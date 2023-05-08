# Hazelcast Jet with Kafka source Demo

## Requirements

* JDK 8 or better
* Hazelcast 5.3.0-BETA-2 or better: https://hazelcast.com/open-source-projects/downloads/
* Hazelcast CLC v5.3.0-BETA-2 or better: https://github.com/hazelcast/hazelcast-commandline-client/releases
* A recent version of Apache Kafka: https://kafka.apache.org/downloads
* Kafkactl: https://github.com/deviceinsight/kafkactl

## Usage

### Building the Project

```
make
```

### Steps 

- Run Hazelcast and Kafka broker
- Build jar by `make build`
- Connect to Hazelcast with CLC 
```
clc config add local cluster.name=dev cluster.address=localhost:5701
```
- Add mappings for SQL
```
cat dessert.sql | clc -c local
```
- Submit JAR
```
clc -c local job submit build/libs/jet-pipeline-sample-1.0-SNAPSHOT-all.jar --name orders
```
- Start producer
```
cd producer && go run main.go
```