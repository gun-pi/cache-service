package com.cacheservice.simplejava;

import com.cacheservice.CacheService;
import com.cacheservice.CacheStatisticsObject;
import com.cacheservice.UtilityAssertions;
import com.cacheservice.simplejava.cachestatistics.CacheStatistics;
import com.cacheservice.simplejava.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Simple Java implementation of cache service interface
 *
 * @param <T>
 */
public class SimpleCacheService<T> implements CacheService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCacheService.class.getName());

    private final Map<String, CachedObject<T>> cache;
    private final ConcurrentSkipListSet<CachedObject<T>> frequencySortedCachedObjects;
    private final Collection<Listener> listeners;
    private final CacheStatistics cacheStatistics;
    private final int maxCacheSize;
    private final TimeService timeService;
    private final long timeoutInSec;


    /**
     * Simple Java cache service constructor
     *
     * @param maxCacheSize    max cache size; if negative value or 0 provided, default value = 100 000
     * @param timeoutInSec    timeout time in secs for removing obsolete cache items
     * @param listeners       listeners
     * @param cacheStatistics cache statistics for simple java cache service
     * @param timeService     time service
     */
    public SimpleCacheService(int maxCacheSize,
                              long timeoutInSec,
                              Collection<Listener> listeners,
                              CacheStatistics cacheStatistics,
                              TimeService timeService) {
        this.maxCacheSize = maxCacheSize > 0 ? maxCacheSize : 100_000;
        this.cache = new ConcurrentHashMap<>(this.maxCacheSize);
        this.frequencySortedCachedObjects = new ConcurrentSkipListSet<>();
        this.timeoutInSec = timeoutInSec > 0 ? timeoutInSec : 5;
        this.listeners = listeners;
        this.cacheStatistics = cacheStatistics;
        this.timeService = timeService;

        runCacheCleanUpPeriodicTask();
        LOGGER.info("CacheService is created");
    }

    /**
     * Get value by key. If there is no value in cache - returns null.
     *
     * @param key key
     * @return value;
     */
    @Override
    public T get(String key) {
        UtilityAssertions.assertInputStringsNotBlankOrNull(key);

        LOGGER.trace("Getting value with key {} from cache", key);
        if (cache.containsKey(key)) {
            CachedObject<T> cachedObject = cache.get(key);
            frequencySortedCachedObjects.remove(cachedObject);
            cachedObject.setLastAccessDateTimeEpochMilli(timeService.getTimeWithSystemDefaultZoneEpochMilli());
            cachedObject.incrementFrequency();
            frequencySortedCachedObjects.add(cachedObject);

            LOGGER.trace("Object with key {} is retrieved from cache", key);
            return cachedObject.getValue();
        }
        LOGGER.trace("Cache does not contain key {}. Returning null", key);
        return null;
    }


    /**
     * Put value by key into cache. If cache already has the key - updates entry's frequency and last access time.
     *
     * @param key   key
     * @param value value
     */
    @Override
    public void put(String key, T value) {
        long methodStartTime = timeService.getTimeWithSystemDefaultZoneEpochMilli();
        UtilityAssertions.assertInputStringsNotBlankOrNull(key);

        LOGGER.trace("Putting value with key {} in cache", key);
        if (cache.containsKey(key)) {
            CachedObject<T> cachedObject = cache.get(key);
            frequencySortedCachedObjects.remove(cachedObject);
            cachedObject.setValue(value);
            cachedObject.setLastAccessDateTimeEpochMilli(timeService.getTimeWithSystemDefaultZoneEpochMilli());
            cachedObject.incrementFrequency();
            frequencySortedCachedObjects.add(cachedObject);
            LOGGER.trace("Cache already has object with key {}. The object has been updated", key);
            return;
        }

        removeLeastFrequencyObjectIfNeeded();

        CachedObject<T> cachedObject = new CachedObject<>(key, value, timeService.getTimeWithSystemDefaultZoneEpochMilli());
        cache.put(key, cachedObject);
        frequencySortedCachedObjects.add(cachedObject);

        long methodEndTime = timeService.getTimeWithSystemDefaultZoneEpochMilli();
        long timeSpentForPuttingValue = methodEndTime - methodStartTime;
        cachedObject.setPuttingValueTime(timeSpentForPuttingValue);
        LOGGER.trace("Object with key '{}' has put into cache. Time spent for putting: {} ms", cachedObject.getKey(), timeSpentForPuttingValue);
        eventHappens(EventType.PUT_NEW_OBJECT, cachedObject);
    }

    /**
     * Return Simple Java cache statistics
     *
     * @return cache statistics object
     */
    @Override
    public CacheStatisticsObject returnCacheStatistics() {
        return cacheStatistics.returnCacheStatistics();
    }

    /**
     * Clean cache from obsolete objects
     */
    @Override
    public void cacheCleanUp() {
        cache.values().parallelStream()
                .filter(cachedObject -> {
                    long lastAccessDateTimeEpochMilli = cachedObject.getLastAccessDateTimeEpochMilli();
                    return timeService.getTimeWithSystemDefaultZoneEpochMilli() - lastAccessDateTimeEpochMilli > timeoutInSec * 1000;
                })
                .forEach(obsoleteCachedObject -> {
                    eventHappens(EventType.REMOVE_OBSOLETE_OBJECT, obsoleteCachedObject);
                    cache.remove(obsoleteCachedObject.getKey());
                    frequencySortedCachedObjects.remove(obsoleteCachedObject);
                });
    }

    /**
     * Removes the least frequency objects if cache size >= cache max size
     */
    private void removeLeastFrequencyObjectIfNeeded() {
        while (cache.size() >= maxCacheSize) {
            CachedObject<T> leastFrequencyCachedObject = frequencySortedCachedObjects.pollFirst();
            if (leastFrequencyCachedObject == null) {
                throw new IllegalArgumentException("frequencySortedCachedObjects set should not contain null!");
            }
            cache.remove(leastFrequencyCachedObject.getKey());
            eventHappens(EventType.REMOVE_LEAST_FREQUENCY_OBJECT, leastFrequencyCachedObject);
        }
    }

    /**
     * Triggers listener event. If listeners is not provided in Cache Service, does nothing.
     *
     * @param eventType    event type
     * @param cachedObject cached object
     */
    private void eventHappens(EventType eventType, CachedObject<T> cachedObject) {
        if (listeners != null) {
            assertInputObjectsNotNull(eventType, cachedObject);

            for (Listener listener : listeners) {
                listener.onEvent(eventType, cachedObject);
            }
        }
    }

    /**
     * Removes all objects that have been accessed more than timeout time.
     * Task runs every 0.5 second.
     */
    private void runCacheCleanUpPeriodicTask() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                cacheCleanUp();
            }
        }, 0, 500);
    }

    /**
     * Builder for Simple Java Cache Service
     *
     * @param <T>
     */
    public static class Builder<T> {

        private Collection<Listener> listeners;
        private CacheStatistics cacheStatistics;
        private int maxCacheSize;
        private TimeService timeService;
        private long timeoutInSec;

        /**
         * Set max cache size to builder
         *
         * @param maxCacheSize max cache size
         * @return builder
         */
        public Builder<T> maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        /**
         * Set timeout time in secs to builder
         *
         * @param timeoutInSec timeout time in secs
         * @return builder
         */
        public Builder<T> timeoutInSec(int timeoutInSec) {
            this.timeoutInSec = timeoutInSec;
            return this;
        }

        /**
         * Set listeners to builder
         *
         * @param listeners listeners
         * @return builder
         */
        public Builder<T> listeners(Collection<Listener> listeners) {
            this.listeners = listeners;
            return this;
        }

        /**
         * Set cache statistics to builder
         *
         * @param cacheStatistics cache statistics
         * @return builder
         */
        public Builder<T> cacheStatistics(CacheStatistics cacheStatistics) {
            this.cacheStatistics = cacheStatistics;
            return this;
        }

        /**
         * Set time service to builder
         *
         * @param timeService time service
         * @return builder
         */
        public Builder<T> timeService(TimeService timeService) {
            this.timeService = timeService;
            return this;
        }

        /**
         * Builds Simple Java Cache service instance
         *
         * @return Simple Java Cache service instance
         */
        public SimpleCacheService<T> build() {
            return new SimpleCacheService<>(
                    maxCacheSize,
                    timeoutInSec,
                    listeners,
                    cacheStatistics,
                    timeService);
        }
    }
}
