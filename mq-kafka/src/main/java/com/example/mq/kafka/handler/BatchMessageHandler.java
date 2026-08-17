package com.example.mq.kafka.handler;

import com.example.mq.kafka.constants.KafkaConsts;
import com.example.mq.kafka.store.ReceivedMessageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * Batch listener: one call per poll, with everything the poll returned. How many records show up
 * depends on how fast they were produced and on spring.kafka.consumer.max-poll-records, so a burst
 * sent in one go usually arrives as a single batch.
 * </p>
 *
 * @author NamHoang
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchMessageHandler {

    private final ReceivedMessageStore store;

    @KafkaListener(topics = KafkaConsts.TOPIC_BATCH, containerFactory = "batchContainerFactory")
    public void handleBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        try {
            log.info("[batch] received {} record(s)", records.size());
            for (ConsumerRecord<String, String> record : records) {
                log.info("[batch]   partition={} offset={} value={}", record.partition(), record.offset(), record.value());
                store.add(record.topic(), record.partition(), record.offset(), record.key(), record.value());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            // One ack for the whole batch, committing the last offset of each partition in it.
            acknowledgment.acknowledge();
        }
    }
}
