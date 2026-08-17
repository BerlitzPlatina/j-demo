package com.example.async.model;

/**
 * <p>
 * What a task reports back: which thread ran it, whether that thread was virtual, the trace id it
 * inherited from the caller and how long it took.
 * </p>
 *
 * @author NamHoang
 */
public record TaskResult(String task, String thread, boolean virtualThread, String traceId, long tookMs) {
}
