package com.cacheservice.simplejava.cachestatistics;

import com.cacheservice.CacheStatisticsObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gathers information about Cache Service:
 * - number of removed obsolete objects
 * - number of removed least frequency objects
 * - putting value time list (to calculate average putting value time)
 */
public class CacheStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheStatistics.class.getName());

    private final AtomicInteger removedObsoleteObjectsNumber = new AtomicInteger();
    private final AtomicInteger removedLeastFrequencyObjectsNumber = new AtomicInteger();
    private final Deque<Long> puttingValueTimeList = new ConcurrentLinkedDeque<>();
    private final int maxCacheSize;

    /**
     * Cache statistics constructor
     *
     * @param maxCacheSize max cache size; if negative value or 0 provided, default value = 100 000
     */
    public CacheStatistics(int maxCacheSize) {
        if (maxCacheSize <= 0) {
            throw new IllegalArgumentException("Max cache size of simple cache service should be positive!");
        }
        this.maxCacheSize = maxCacheSize;
        LOGGER.info("Cache Statistics is created");
    }

    /**
     * Increments number of removed obsolete objects
     */
    public void incrementRemovedObsoleteObjectsNumber() {
        removedObsoleteObjectsNumber.incrementAndGet();
    }

    /**
     * Increments number of removed least frequency objects
     */
    public void incrementRemovedLeastFrequencyObjectsNumber() {
        removedLeastFrequencyObjectsNumber.incrementAndGet();
    }

    /**
     * Adds putting value time to the putting value time list for further calculation
     * of putting value average time
     *
     * @param valuePuttingTime value putting time
     */
    public void addPuttingValueTime(long valuePuttingTime) {
        if (valuePuttingTime < 0) {
            throw new IllegalArgumentException("Value putting time can not be negative!");
        }

        while (puttingValueTimeList.size() >= maxCacheSize) {
            puttingValueTimeList.pollLast();
        }
        puttingValueTimeList.addFirst(valuePuttingTime);
    }

    /**
     * Return Simple Java cache statistics
     *
     * @return cache statistics object
     */
    public CacheStatisticsObject returnCacheStatistics() {
        int evictionCount = removedObsoleteObjectsNumber.intValue() + removedLeastFrequencyObjectsNumber.intValue();
        double puttingValueTimeSum = puttingValueTimeList.parallelStream().mapToLong(x -> x).sum();
        double averageLoadPenaltyInSecs = puttingValueTimeSum / (puttingValueTimeList.size() * 1000);
        return new CacheStatisticsObject(evictionCount, round(averageLoadPenaltyInSecs, 2));
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
