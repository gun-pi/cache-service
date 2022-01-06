package com.cacheservice;

import com.cacheservice.guava.GuavaCacheService;
import com.cacheservice.simplejava.SimpleCacheService;
import com.cacheservice.simplejava.TimeService;
import com.cacheservice.simplejava.cachestatistics.CacheStatistics;
import com.cacheservice.simplejava.listener.CacheStatisticsListener;
import com.cacheservice.simplejava.listener.Listener;
import com.cacheservice.simplejava.listener.RemovalLoggingListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CacheServiceSize1Test {

    static Stream<CacheService<CacheServiceTestObject>> cacheServiceProvider() {
        int maxCacheSize = 1;
        int timeout = 5;
        int concurrencyLevel = 1;

        CacheService<CacheServiceTestObject> guavaCacheService = new GuavaCacheService.Builder<CacheServiceTestObject>()
                .maxCacheSize(maxCacheSize)
                .timeoutInSec(timeout)
                .concurrencyLevel(concurrencyLevel)
                .build();

        CacheStatistics cacheStatistics = new CacheStatistics(maxCacheSize);
        Listener removalLoggingListener = new RemovalLoggingListener();
        Listener cacheStatisticsListener = new CacheStatisticsListener(cacheStatistics);
        TimeService testTimeService = new TestTimeService();
        CacheService<CacheServiceTestObject> simpleCacheService = new SimpleCacheService.Builder<CacheServiceTestObject>()
                .maxCacheSize(maxCacheSize)
                .timeoutInSec(timeout)
                .listeners(List.of(removalLoggingListener, cacheStatisticsListener))
                .cacheStatistics(cacheStatistics)
                .timeService(testTimeService)
                .build();

        return Stream.of(guavaCacheService, simpleCacheService);
    }

    @ParameterizedTest
    @DisplayName("Put null object to cache and get object back checking null")
    @MethodSource("cacheServiceProvider")
    void checkNullValueOfCachedObject(CacheService<CacheServiceTestObject> cacheService) {
        String key = "key_1";
        cacheService.put(key, null);

        CacheServiceTestObject returnedValue = cacheService.get("key_1");

        assertNull(returnedValue);
    }

    @ParameterizedTest
    @DisplayName("Put object to cache and get object back checking value")
    @MethodSource("cacheServiceProvider")
    void checkValueOfCachedObject(CacheService<CacheServiceTestObject> cacheService) {
        String key = "key_1";
        CacheServiceTestObject value = new CacheServiceTestObject("value_1");
        cacheService.put(key, value);

        CacheServiceTestObject returnedValue = cacheService.get(key);

        assertEquals(value.getField(), returnedValue.getField());
    }

    @ParameterizedTest
    @DisplayName("Put 2 objects and check evicted least frequency object")
    @MethodSource("cacheServiceProvider")
    void getNullObjectWhenCacheMaxSizeExceeded(CacheService<CacheServiceTestObject> cacheService) {
        String key = "key_1";
        CacheServiceTestObject value = new CacheServiceTestObject("value_1");
        cacheService.put(key, value);

        String key2 = "key_2";
        CacheServiceTestObject value2 = new CacheServiceTestObject("value_2");
        cacheService.put(key2, value2);

        assertNull(cacheService.get("key_1"));
        assertEquals("value_2", cacheService.get("key_2").getField());
    }

    @ParameterizedTest
    @DisplayName("Get null retrieving obsolete object")
    @MethodSource("cacheServiceProvider")
    void getNullWhenGetObsoleteObject(CacheService<CacheServiceTestObject> cacheService) throws InterruptedException {
        String key = "key_1";
        CacheServiceTestObject value = new CacheServiceTestObject("value_1");
        cacheService.put(key, value);

        TimeUnit.SECONDS.sleep(6);

        assertNull(cacheService.get("key_1"));
    }

    @ParameterizedTest
    @DisplayName("Put 100 items to cache with size 1 getting least frequency objects removed," +
            "then check cache statistics information about evicted objects and average putting time.")
    @MethodSource("cacheServiceProvider")
    void checkStatisticsAfterRemovingLeastFrequency(CacheService<CacheServiceTestObject> cacheService) {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });

        CacheStatisticsObject cacheStatisticsObject = cacheService.returnCacheStatistics();

        String expectedCacheStatistics = "Statistics:\n" +
                "Eviction count = 99\n" +
                "Average load penalty = 0.0 ms\n";
        assertEquals(expectedCacheStatistics, cacheStatisticsObject.toString());
    }
}
