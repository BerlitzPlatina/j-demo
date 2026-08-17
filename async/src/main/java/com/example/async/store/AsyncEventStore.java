package com.example.async.store;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * <p>
 * Records what background tasks did after the HTTP response was already sent. Without it a
 * fire and forget call has nothing to show: the caller gets an empty 202 and the outcome only
 * exists in the log. Bounded and in memory, a demo aid rather than storage.
 * </p>
 *
 * @author NamHoang
 */
@Component
public class AsyncEventStore {

    private static final int MAX_SIZE = 100;

    /**
     * Written by pool threads, read by the controller thread, so it has to be thread safe.
     */
    private final Deque<Event> events = new ConcurrentLinkedDeque<>();

    public void add(String type, String task, String thread, String detail) {
        events.addFirst(Event.builder()
                .type(type)
                .task(task)
                .thread(thread)
                .detail(detail)
                .at(System.currentTimeMillis())
                .build());

        while (events.size() > MAX_SIZE) {
            events.pollLast();
        }
    }

    /**
     * Newest first.
     */
    public List<Event> list() {
        return new ArrayList<>(events);
    }

    public int clear() {
        int size = events.size();
        events.clear();
        return size;
    }

    @Data
    @Builder
    public static class Event {
        /**
         * done, failed, or rejected: enough to tell a normal completion from a task that blew up.
         */
        private String type;
        private String task;
        private String thread;
        private String detail;
        private long at;
    }
}
