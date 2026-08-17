package com.example.mq.kafka.constants;

/**
 * <p>
 * Kafka constant pool: topic names, the consumer group and the container defaults.
 * </p>
 *
 * @author NamHoang
 */
public interface KafkaConsts {
    /**
     * Number of partitions used for the topics that demonstrate key based routing. The listener
     * concurrency is aligned with it, so each partition gets its own consumer thread.
     */
    int DEFAULT_PARTITION_NUM = 3;

    /**
     * Every listener in this module joins the same group, so each record is handled once.
     */
    String GROUP_ID = "mq-kafka-demo";

    /**
     * Plain text topic, consumed one record at a time with a manual ack.
     */
    String TOPIC_SIMPLE = "demo.simple";

    /**
     * Multi partition topic: the message key decides the partition, so records with the same key
     * keep their relative order.
     */
    String TOPIC_KEYED = "demo.keyed";

    /**
     * JSON topic, serialized by JsonSerializer and read back as a {@code MessageStruct}.
     */
    String TOPIC_JSON = "demo.json";

    /**
     * Consumed by a batch listener: the poll returns a list of records instead of a single one.
     */
    String TOPIC_BATCH = "demo.batch";
}
