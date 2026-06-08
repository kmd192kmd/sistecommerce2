package com.example.java.common.util;

import org.springframework.stereotype.Component;

/**
 * Twitter Snowflake ID Generator.
 * 64-bit ID: 1 bit (unused) + 41 bits (timestamp) + 10 bits (worker ID) + 12 bits (sequence)
 */
@Component
public class SnowflakeIdGenerator {

    // Epoch: 2026-01-01 00:00:00 KST (in milliseconds)
    private final long epoch = 1767225600000L;

    private final long workerIdBits = 10L; 
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long sequenceBits = 12L; 

    private final long workerIdShift = sequenceBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private final long workerId = 1L; // default worker node ID (in clustered setups, this can be configured via environment)

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << timestampLeftShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
