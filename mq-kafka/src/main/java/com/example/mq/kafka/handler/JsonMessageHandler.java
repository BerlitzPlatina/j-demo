package com.example.mq.kafka.handler;

import com.example.mq.kafka.constants.KafkaConsts;
import com.example.mq.kafka.message.MessageStruct;
import com.example.mq.kafka.store.ReceivedMessageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Consumes the JSON topic. The payload arrives already deserialized because the container factory
 * is wired with a JsonDeserializer for {@link MessageStruct}.
 * </p>
 *
 * @author NamHoang
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonMessageHandler {

    private final ReceivedMessageStore store;

    @KafkaListener(topics = KafkaConsts.TOPIC_JSON, containerFactory = "jsonContainerFactory")
    public void handleJsonMessage(ConsumerRecord<String, MessageStruct> record, Acknowledgment acknowledgment) {
        try {
            MessageStruct payload = record.value();
            long latency = payload.getSentAt() == null ? -1 : System.currentTimeMillis() - payload.getSentAt();
            log.info("[json] partition={} offset={} payload={} latencyMs={}",
                    record.partition(), record.offset(), payload, latency);
            store.add(record.topic(), record.partition(), record.offset(), record.key(), payload);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
