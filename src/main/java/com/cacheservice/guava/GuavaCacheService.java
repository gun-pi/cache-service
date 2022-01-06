package com.cacheservice.guava;

import com.cacheservice.CacheService;
import com.cacheservice.CacheStatisticsObject;
import com.cacheservice.UtilityAssertions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Guava implementation of cache service interface
 *
 * @param <T>
 */
public class GuavaCacheService<T> implements CacheService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuavaCacheService.class.getName());

    private final Cache<String, CachedObject<T>> cache;

    /**
     * Guava cache service constructor
     *
     * @param maxCacheSize     max cache size; if negative value or 0 provided, default value = 100 000
     * @param timeoutInSec     timeout time in secs for removing obsolete cache items
     * @param concurrencyLevel concurrency level; if negative value or 0 provided, default value = 1
     */
    public GuavaCacheService(long maxCacheSize, int timeoutInSec, int concurrencyLevel) {
        maxCacheSize = maxCacheSize > 0 ? maxCacheSize : 100_000;
        concurrencyLevel = concurrencyLevel > 0 ? concurrencyLevel : 1;
        timeoutInSec = timeoutInSec > 0 ? timeoutInSec : 5;

        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrencyLevel)
                .maximumSize(maxCacheSize)
                .expireAfterWrite(timeoutInSec, TimeUnit.SECONDS)
                .recordStats()
                .removalListener(notification ->
                        LOGGER.trace("Object with key {} is being removed. Cause: {}", notification.getKey(), notification.getCause()))
                .build();
        LOGGER.info("CacheService is created");
    }

    /**
     * Get value by key. If there is no value in cache - returns null.
     *
     * @param key key
     * @return value;
     */
    public T get(String key) {
        UtilityAssertions.assertInputStringsNotBlankOrNull(key);

        LOGGER.trace("Getting value with key {} from cache", key);
        CachedObject<T> cachedObject = cache.getIfPresent(key);

        if (cachedObject != null) {
            LOGGER.trace("The object with key {} is retrieved from cache", key);
            return cachedObject.getValue();
        }
        LOGGER.trace("Cache does not have key {}. Returning null", key);
        return null;
    }

    /**
     * Put value by key in cache
     *
     * @param key   key
     * @param value value
     */
    public void put(String key, T value) {
        UtilityAssertions.assertInputStringsNotBlankOrNull(key);

        CachedObject<T> cachedObject = new CachedObject<>(value);

        LOGGER.trace("Putting object with key {} into cache", key);
        cache.put(key, cachedObject);
    }

    /**
     * Performs full cache clean up from obsolete cache items
     * (background clean up performs partial cleaning only)
     */
    public void cacheCleanUp() {
        LOGGER.trace("Cleaning cache from obsolete entries");
        cache.cleanUp();
    }

    /**
     * Return Guava cache statistics
     *
     * @return cache statistics object
     */
    public CacheStatisticsObject returnCacheStatistics() {
        return new CacheStatisticsObject(cache.stats().evictionCount(), cache.stats().averageLoadPenalty());
    }

    /**
     * Builder for Guava Cache Service
     *
     * @param <T>
     */
    public static class Builder<T> {

        private long maxCacheSize;
        private int timeoutInSec;
        private int concurrencyLevel;

        /**
         * Set max cache size to builder
         *
         * @param maxCacheSize max cache size
         * @return builder
         */
        public GuavaCacheService.Builder<T> maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        /**
         * Set timeout time in secs to builder
         *
         * @param timeoutInSec timeout time in secs
         * @return builder
         */
        public GuavaCacheService.Builder<T> timeoutInSec(int timeoutInSec) {
            this.timeoutInSec = timeoutInSec;
            return this;
        }

        /**
         * Set concurrency level to builder
         *
         * @param concurrencyLevel concurrency level
         * @return builder
         */
        public GuavaCacheService.Builder<T> concurrencyLevel(int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        /**
         * Builds Guava Cache service instance
         *
         * @return Guava Cache service instance
         */
        public GuavaCacheService<T> build() {
            return new GuavaCacheService<>(maxCacheSize, timeoutInSec, concurrencyLevel);
        }
    }
}
