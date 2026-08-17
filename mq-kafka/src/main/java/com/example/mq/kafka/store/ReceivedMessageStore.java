package com.example.mq.kafka.store;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * <p>
 * Keeps the last consumed records in memory so the demo can be followed over HTTP instead of by
 * tailing the log. Bounded on purpose, and lost on restart: this is a demo aid, not storage.
 * </p>
 *
 * @author NamHoang
 */
@Component
public class ReceivedMessageStore {

    private static final int MAX_SIZE = 100;

    /**
     * Listener threads write concurrently (the ack factory runs three of them), the controller
     * reads, so the collection has to be thread safe.
     */
    private final Deque<Received> received = new ConcurrentLinkedDeque<>();

    public void add(String topic, int partition, long offset, String key, Object payload) {
        received.addFirst(Received.builder()
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .key(key)
                .payload(payload)
                .receivedAt(System.currentTimeMillis())
                .build());

        // Trim from the tail, the newest entries are at the head.
        while (received.size() > MAX_SIZE) {
            received.pollLast();
        }
    }

    /**
     * Newest first.
     */
    public List<Received> list() {
        return new ArrayList<>(received);
    }

    public int clear() {
        int size = received.size();
        received.clear();
        return size;
    }

    @Data
    @Builder
    public static class Received {
        private String topic;
        private int partition;
        private long offset;
        private String key;
        private Object payload;
        private long receivedAt;
    }
}
