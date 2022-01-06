package com.cacheservice.simplejava;

import com.cacheservice.UtilityAssertions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry object of Cache Service.
 */
public class CachedObject<T> implements Comparable<CachedObject<T>> {

    private final String key;
    private T value;
    private final AtomicInteger frequency;
    private long lastAccessDateTimeEpochMilli;
    private long puttingValueTime;

    /**
     * Cached object constructor
     *
     * @param key                          key
     * @param value                        value
     * @param lastAccessDateTimeEpochMilli last access date time epoch in millis
     */
    CachedObject(String key, T value, long lastAccessDateTimeEpochMilli) {
        UtilityAssertions.assertInputStringsNotBlankOrNull(key);
        if (lastAccessDateTimeEpochMilli <= 0) {
            throw new IllegalArgumentException("Last access date time value should be positive!");
        }

        this.key = key;
        this.value = value;
        this.lastAccessDateTimeEpochMilli = lastAccessDateTimeEpochMilli;
        this.frequency = new AtomicInteger(1);
    }

    public String getKey() {
        return key;
    }

    T getValue() {
        return value;
    }

    AtomicInteger getFrequency() {
        return frequency;
    }

    long getLastAccessDateTimeEpochMilli() {
        return lastAccessDateTimeEpochMilli;
    }

    public long getPuttingValueTime() {
        return puttingValueTime;
    }

    void setValue(T value) {
        this.value = value;
    }

    void setPuttingValueTime(long puttingValueTime) {
        this.puttingValueTime = puttingValueTime;
    }

    void setLastAccessDateTimeEpochMilli(long lastAccessDateTimeEpochMilli) {
        this.lastAccessDateTimeEpochMilli = lastAccessDateTimeEpochMilli;
    }

    void incrementFrequency() {
        frequency.incrementAndGet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CachedObject)) return false;

        CachedObject<?> that = (CachedObject<?>) o;

        return getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public String toString() {
        Instant lastAccessInstant = Instant.ofEpochMilli(lastAccessDateTimeEpochMilli);
        ZonedDateTime lastAccessZonedDateTime = ZonedDateTime.ofInstant(lastAccessInstant, ZoneId.systemDefault());
        return "CachedObject{" +
                "key='" + key + '\'' +
                ", frequency=" + frequency +
                ", lastAccessDateTime=" + lastAccessZonedDateTime +
                '}';
    }

    @Override
    public int compareTo(CachedObject<T> that) {
        if (this.getKey().equals(that.getKey())) {
            return 0;
        }

        int frequencyComparison = Integer.compare(this.getFrequency().intValue(), that.getFrequency().intValue());
        if (frequencyComparison != 0) {
            return frequencyComparison;
        }

        int lastAccessComparison = Long.compare(
                this.getLastAccessDateTimeEpochMilli(), that.getLastAccessDateTimeEpochMilli()
        );
        if (lastAccessComparison != 0) {
            return lastAccessComparison;
        }

        return this.getKey().compareTo(that.getKey());
    }
}
