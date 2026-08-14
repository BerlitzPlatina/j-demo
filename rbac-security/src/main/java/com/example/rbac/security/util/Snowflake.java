package com.example.rbac.security.util;

/**
 * <p>
 * Twitter snowflake id generator, a drop-in replacement for hutool's {@code cn.hutool.core.lang.Snowflake}.
 * </p>
 * <p>
 * A generated id is a 64 bit long laid out as: 1 unused sign bit, 41 bits of milliseconds since
 * {@link #EPOCH}, 5 bits of datacenter id, 5 bits of worker id and 12 bits of sequence. That gives
 * each worker 4096 ids per millisecond, and ids that increase over time so they index well as a
 * primary key.
 * </p>
 *
 * @author NamHoang
 */
public class Snowflake {

    /**
     * Ids are counted from this instant rather than 1970 so the 41 bit timestamp lasts longer.
     * 2018-01-01T00:00:00Z, matching the era of the data this module was built against.
     */
    private static final long EPOCH = 1514764800000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake(long workerId, long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId must be between 0 and " + MAX_DATACENTER_ID);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * Generates the next id. Synchronized because the sequence and the last timestamp are shared
     * mutable state and this generator is used as a singleton bean.
     *
     * @return a new id, larger than every id returned before it on this instance
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            // The clock moved backwards, so continuing would hand out ids that were already used.
            throw new IllegalStateException("Clock moved backwards, refusing to generate an id for " + (lastTimestamp - timestamp) + "ms");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // This millisecond is exhausted; spin until the next one starts.
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (datacenterId << DATACENTER_ID_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
