package com.cacheservice;

public interface CacheService<T> {

    T get(String key);

    void put(String key, T value);

    CacheStatisticsObject returnCacheStatistics();

    void cacheCleanUp();
}
