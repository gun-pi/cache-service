package com.cacheservice.simplejava.listener;

import com.cacheservice.simplejava.CachedObject;
import com.cacheservice.simplejava.EventType;
import com.cacheservice.simplejava.cachestatistics.CacheStatistics;

/**
 * Cache statistics listener for listening events from Cache Service and triggering according methods
 * of Cache Statistics to update statistics information
 */
public class CacheStatisticsListener implements Listener {

    private final CacheStatistics cacheStatistics;

    /**
     * Cache statistics listener constructor
     *
     * @param cacheStatistics cache statistics
     */
    public CacheStatisticsListener(CacheStatistics cacheStatistics) {
        this.cacheStatistics = cacheStatistics;
    }

    /**
     * Listener action when event is happening. Triggers methods of Cache Statistics
     * to update statistics information
     *
     * @param eventType    event type
     * @param cachedObject cached object
     */
    @Override
    public void onEvent(EventType eventType, CachedObject<?> cachedObject) {
        if (eventType == EventType.REMOVE_OBSOLETE_OBJECT) {
            cacheStatistics.incrementRemovedObsoleteObjectsNumber();
        } else if (eventType == EventType.REMOVE_LEAST_FREQUENCY_OBJECT) {
            cacheStatistics.incrementRemovedLeastFrequencyObjectsNumber();
        } else if (eventType == EventType.PUT_NEW_OBJECT) {
            cacheStatistics.addPuttingValueTime(cachedObject.getPuttingValueTime());
        }
    }
}
