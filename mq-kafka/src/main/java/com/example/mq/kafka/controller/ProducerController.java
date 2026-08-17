package com.example.mq.kafka.controller;

import com.example.mq.kafka.constants.KafkaConsts;
import com.example.mq.kafka.message.MessageStruct;
import com.example.mq.kafka.store.ReceivedMessageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Produces messages, so the listeners in this module have something to consume, and exposes what
 * was consumed so the round trip can be seen without reading the log.
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
@Slf4j
public class ProducerController {

    /**
     * How long a send may take before the endpoint gives up. The producer keeps retrying in the
     * background beyond this, the timeout only bounds the HTTP call.
     */
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaTemplate<String, MessageStruct> jsonKafkaTemplate;

    private final KafkaAdmin kafkaAdmin;

    private final ReceivedMessageStore store;

    /**
     * Simplest case: no key, single partition topic, consumed one record at a time.
     */
    @PostMapping("/send")
    public Map<String, Object> send(@RequestParam(defaultValue = "hello kafka") String message) {
        return await(kafkaTemplate.send(KafkaConsts.TOPIC_SIMPLE, message), message);
    }

    /**
     * With a key. The producer hashes it to pick a partition, so the same key always lands on the
     * same partition and its records stay in order relative to each other. Send with key=a a few
     * times, then key=b, and compare the partitions in the response.
     */
    @PostMapping("/send-key")
    public Map<String, Object> sendWithKey(@RequestParam(defaultValue = "user-1") String key,
                                           @RequestParam(defaultValue = "hello keyed") String message) {
        Map<String, Object> result = await(kafkaTemplate.send(KafkaConsts.TOPIC_KEYED, key, message), message);
        result.put("key", key);
        return result;
    }

    /**
     * Bypasses the partitioner and names the partition directly, which is what you would do to pin
     * traffic to a partition on purpose.
     *
     * @param partition 0 based, must be below {@link KafkaConsts#DEFAULT_PARTITION_NUM}
     */
    @PostMapping("/send-partition")
    public Map<String, Object> sendToPartition(@RequestParam(defaultValue = "0") int partition,
                                               @RequestParam(required = false) String key,
                                               @RequestParam(defaultValue = "hello partition") String message) {
        if (partition < 0 || partition >= KafkaConsts.DEFAULT_PARTITION_NUM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "partition must be between 0 and " + (KafkaConsts.DEFAULT_PARTITION_NUM - 1));
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConsts.TOPIC_KEYED, partition, key, message);
        Map<String, Object> result = await(kafkaTemplate.send(record), message);
        result.put("key", key);
        return result;
    }

    /**
     * JSON payload, serialized by JsonSerializer and read back as an object by the listener.
     */
    @PostMapping("/send-json")
    public Map<String, Object> sendJson(@RequestBody(required = false) MessageStruct body) {
        MessageStruct payload = body == null ? MessageStruct.builder().message("hello json").amount(1).build() : body;
        // Stamped here rather than trusting the caller, so the consumer side latency in the log is
        // measured from the moment the message was actually produced.
        payload.setSentAt(System.currentTimeMillis());

        Map<String, Object> result = await(jsonKafkaTemplate.send(KafkaConsts.TOPIC_JSON, payload), payload);
        result.put("payload", payload);
        return result;
    }

    /**
     * Fires a burst at the batch topic. The sends are queued first and waited on afterwards, so the
     * producer can group them into few requests and the consumer sees them as one batch.
     *
     * @param count how many messages to produce
     */
    @PostMapping("/send-batch")
    public Map<String, Object> sendBatch(@RequestParam(defaultValue = "10") int count,
                                         @RequestParam(defaultValue = "batch message") String message) {
        if (count < 1 || count > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count must be between 1 and 1000");
        }

        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            futures.add(kafkaTemplate.send(KafkaConsts.TOPIC_BATCH, message + " #" + i));
        }

        List<Long> offsets = new ArrayList<>(count);
        for (CompletableFuture<SendResult<String, String>> future : futures) {
            offsets.add(waitFor(future).getRecordMetadata().offset());
        }

        log.info("Sent {} record(s) to {}", count, KafkaConsts.TOPIC_BATCH);
        return Map.of("topic", KafkaConsts.TOPIC_BATCH, "count", count,
                "firstOffset", offsets.getFirst(), "lastOffset", offsets.getLast(), "sent", true);
    }

    /**
     * What the listeners have consumed so far, newest first. In memory only, capped, cleared on
     * restart.
     */
    @GetMapping("/received")
    public List<ReceivedMessageStore.Received> received() {
        return store.list();
    }

    @DeleteMapping("/received")
    public Map<String, Object> clearReceived() {
        return Map.of("cleared", store.clear());
    }

    /**
     * Asks the broker about the demo topics: partition count, leader and replicas per partition.
     * Also a quick way to check the application really is talking to Kafka.
     */
    @GetMapping("/topics")
    public Map<String, Object> topics() {
        Map<String, TopicDescription> descriptions;
        try {
            descriptions = kafkaAdmin.describeTopics(KafkaConsts.TOPIC_SIMPLE, KafkaConsts.TOPIC_KEYED,
                    KafkaConsts.TOPIC_JSON, KafkaConsts.TOPIC_BATCH);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot reach Kafka: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        descriptions.forEach((name, description) -> {
            List<Map<String, Object>> partitions = description.partitions().stream()
                    .map(p -> Map.<String, Object>of(
                            "partition", p.partition(),
                            "leader", p.leader() == null ? "none" : p.leader().idString(),
                            "replicas", p.replicas().size()))
                    .toList();
            result.put(name, Map.of("partitions", partitions.size(), "detail", partitions));
        });
        return result;
    }

    private Map<String, Object> await(CompletableFuture<? extends SendResult<String, ?>> future, Object message) {
        SendResult<String, ?> result = waitFor(future);
        var metadata = result.getRecordMetadata();
        log.info("Sent to {} partition={} offset={}: {}", metadata.topic(), metadata.partition(), metadata.offset(), message);

        // LinkedHashMap rather than Map.of so callers can add fields and the order stays readable.
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("topic", metadata.topic());
        response.put("partition", metadata.partition());
        response.put("offset", metadata.offset());
        response.put("message", message);
        response.put("sent", true);
        return response;
    }

    private <T> T waitFor(CompletableFuture<T> future) {
        try {
            return future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Interrupted while sending", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Send failed: " + e.getMessage(), e);
        }
    }
}
