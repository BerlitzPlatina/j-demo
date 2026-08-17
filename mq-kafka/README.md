# mq-kafka

Kafka demo module: a REST API produces messages, listeners in the same application consume them,
and everything consumed is kept in memory so the round trip can be seen over HTTP.

## Broker

The application reads the repository root `.env`:

```properties
KAFKA_CLUSTER_ID=CbRNsi2nS7qzy_Fmo9b2fQ
KAFKA_PORT=9094
KAFKA_AUTO_CREATE_TOPICS=true
KAFKA_HOST=172.17.0.1
```

`KAFKA_HOST` is the docker bridge gateway, because the broker runs in the compose stack on the host
while the application runs inside the devcontainer; `localhost` there is the devcontainer itself.

**The broker must advertise that same address.** The port is reachable, but if the broker advertises
`localhost:9094` the client gets that address back in the metadata response and then connects to the
wrong host — startup fails with `Could not configure topics` / `TimeoutException`. In the compose
service set:

```yaml
KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,EXTERNAL://172.17.0.1:9094
```

(the exact listener names depend on `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP`). Running the
application from the host instead of the devcontainer works with `localhost` advertised and
`KAFKA_HOST=localhost`.

`KAFKA_AUTO_CREATE_TOPICS` is not needed: the four topics are declared as `NewTopic` beans in
[KafkaConfig.java](src/main/java/com/example/mq/kafka/config/KafkaConfig.java), so `demo.keyed`
really gets its 3 partitions instead of the broker default of 1.

## Run

```bash
./mvnw spring-boot:run          # http://localhost:8080
```

## Topics

| Topic | Partitions | Consumed by | Shows |
|---|---|---|---|
| `demo.simple` | 1 | `MessageHandler#handleMessage` | single record, manual ack |
| `demo.keyed` | 3 | `MessageHandler#handleKeyedMessage` | key → partition routing, ordering, parallel consumers |
| `demo.json` | 1 | `JsonMessageHandler` | JSON serde into `MessageStruct` |
| `demo.batch` | 1 | `BatchMessageHandler` | batch listener, one call per poll |

All listeners share the group `mq-kafka-demo` and commit offsets themselves
(`AckMode.MANUAL_IMMEDIATE`).

## API

Base path `/api/kafka`.

```bash
# Simple send
curl -X POST 'localhost:8080/api/kafka/send?message=hello demo'
# -> {"topic":"demo.simple","partition":0,"offset":0,"message":"hello demo","sent":true}

# Key based routing: same key always lands on the same partition, in order
curl -X POST 'localhost:8080/api/kafka/send-key?key=user-a&message=a1'
curl -X POST 'localhost:8080/api/kafka/send-key?key=user-b&message=b1'
# Keys hash to a partition, so two different keys can share one; try a few to see the spread.

# Pin to a partition explicitly (0..2), bypassing the partitioner
curl -X POST 'localhost:8080/api/kafka/send-partition?partition=2&message=pinned'

# JSON payload
curl -X POST localhost:8080/api/kafka/send-json \
  -H 'Content-Type: application/json' \
  -d '{"message":"order created","amount":42}'
# sentAt is stamped by the producer; the consumer logs the end to end latency.

# Burst, arrives at the batch listener as one batch
curl -X POST 'localhost:8080/api/kafka/send-batch?count=5'

# What the listeners consumed, newest first (in memory, capped at 100, lost on restart)
curl localhost:8080/api/kafka/received
curl -X DELETE localhost:8080/api/kafka/received

# Ask the broker about the demo topics — also a quick connectivity check
curl localhost:8080/api/kafka/topics
```

## Notes

- The producer runs with `acks=all` and idempotence on, so a retry cannot duplicate a record.
- `linger.ms=20` holds records back briefly so a burst is batched into fewer requests, which is what
  makes the batch listener receive them together.
- `auto-offset-reset=earliest`: a new consumer group starts at the beginning of the topic, so
  messages produced before startup are still delivered.
- Spring Boot's auto configured `KafkaTemplate` backs off as soon as any `KafkaTemplate` bean
  exists, so both the String and the JSON template are declared explicitly.
