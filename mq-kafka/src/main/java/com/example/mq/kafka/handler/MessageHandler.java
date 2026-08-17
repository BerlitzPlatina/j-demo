package com.example.mq.kafka.handler;

import com.example.mq.kafka.constants.KafkaConsts;
import com.example.mq.kafka.store.ReceivedMessageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Single record listeners for the plain text topics. Both commit the offset themselves, which is
 * what MANUAL_IMMEDIATE on the container factory is for.
 * </p>
 *
 * @author NamHoang
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageHandler {

    private final ReceivedMessageStore store;

    @KafkaListener(topics = KafkaConsts.TOPIC_SIMPLE, containerFactory = "ackContainerFactory")
    public void handleMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            log.info("[simple] partition={} offset={} value={}", record.partition(), record.offset(), record.value());
            store.add(record.topic(), record.partition(), record.offset(), record.key(), record.value());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            // Committed in a finally block: a failed record still moves the offset forward, which
            // keeps the demo from replaying the same poison message forever. A real consumer would
            // route it to a dead letter topic instead.
            acknowledgment.acknowledge();
        }
    }

    /**
     * Same topic layout, but three partitions. Send several messages with the same key and they all
     * land on one partition, handled by one thread and therefore in order; different keys spread
     * across the partitions.
     */
    @KafkaListener(topics = KafkaConsts.TOPIC_KEYED, containerFactory = "ackContainerFactory")
    public void handleKeyedMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            log.info("[keyed] thread={} partition={} offset={} key={} value={}",
                    Thread.currentThread().getName(), record.partition(), record.offset(), record.key(), record.value());
            store.add(record.topic(), record.partition(), record.offset(), record.key(), record.value());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
