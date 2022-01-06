package com.cacheservice;

/**
 * Cache statistics object. Has 2 fields:
 * - eviction count number
 * - average load penalty in secs
 */
public class CacheStatisticsObject {

    private final long evictionCount;
    private final double averageLoadPenalty;

    /**
     * Cache statistics constructor
     *
     * @param evictionCount      eviction count
     * @param averageLoadPenalty average load penalty
     */
    public CacheStatisticsObject(long evictionCount, double averageLoadPenalty) {
        this.evictionCount = evictionCount;
        this.averageLoadPenalty = averageLoadPenalty;
    }

    @Override
    public String toString() {
        return "Statistics:\n" +
                "Eviction count = " + evictionCount + "\n" +
                "Average load penalty = " + averageLoadPenalty + " ms\n";
    }
}
