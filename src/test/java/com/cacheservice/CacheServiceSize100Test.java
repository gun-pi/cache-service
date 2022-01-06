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

class CacheServiceSize100Test {

    static Stream<CacheService<CacheServiceTestObject>> cacheServiceProvider() {
        int maxCacheSize = 100;
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
    @DisplayName("Put null to cache and get objects back checking null")
    @MethodSource("cacheServiceProvider")
    void checkNullValueOfCachedObjects(CacheService<CacheServiceTestObject> cacheService) {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    cacheService.put(key, null);
                });

        IntStream.range(0, 100)
                .forEach(x -> {
                    CacheServiceTestObject returnedValue = cacheService.get("key_" + x);
                    assertNull(returnedValue);
                });
    }

    @ParameterizedTest
    @DisplayName("Put object to cache and get object back checking value")
    @MethodSource("cacheServiceProvider")
    void checkValueOfCachedObject(CacheService<CacheServiceTestObject> cacheService) {
        String key = "key";
        CacheServiceTestObject value = new CacheServiceTestObject("value");
        cacheService.put(key, value);

        CacheServiceTestObject returnedValue = cacheService.get(key);

        assertEquals(value.getField(), returnedValue.getField());
    }

    @ParameterizedTest
    @DisplayName("Put objects to cache and get objects back checking value")
    @MethodSource("cacheServiceProvider")
    void checkValueOfCachedObjects(CacheService<CacheServiceTestObject> cacheService) {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });

        IntStream.range(0, 100)
                .forEach(x -> {
                    CacheServiceTestObject returnedValue = cacheService.get("key_" + x);
                    assertEquals("value_" + x, returnedValue.getField());
                });
    }

    @ParameterizedTest
    @DisplayName("Put 100 objects, get first 99 objects (updating their frequency), put 1 object exceeding max cache size " +
            "and check evicted least frequency object with number 99")
    @MethodSource("cacheServiceProvider")
    void getNullObjectWhenCacheMaxSizeExceeded(CacheService<CacheServiceTestObject> cacheService) {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });
        IntStream.range(0, 99)
                .forEach(x -> cacheService.get("key_" + x));

        String key = "key_" + 100;
        CacheServiceTestObject value = new CacheServiceTestObject("value_" + 100);
        cacheService.put(key, value);

        assertNull(cacheService.get("key_" + 99));
    }

    @ParameterizedTest
    @DisplayName("Put 100 objects, get objects 50-100, put 50 objects again exceeding max cache size " +
            "and check that first 50 objects were evicted and others were not evicted")
    @MethodSource("cacheServiceProvider")
    void checkObjectsWhenCacheMaxSizeExceeded(CacheService<CacheServiceTestObject> cacheService) {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });
        IntStream.range(50, 100)
                .forEach(x -> cacheService.get("key_" + x));
        IntStream.range(100, 150)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });

        IntStream.range(0, 50)
                .forEach(x -> assertNull(cacheService.get("key_" + x)));
        IntStream.range(50, 150)
                .forEach(x -> {
                    CacheServiceTestObject returnedValue = cacheService.get("key_" + x);
                    assertEquals("value_" + x, returnedValue.getField());
                });
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
    @DisplayName("Put 4 objects, wait 4 secs, put first 2 objects once again (updating items in cache), wait 4 secs and check " +
            "first 2 object were not deleted as obsolete and last 2 objects were deleted")
    @MethodSource("cacheServiceProvider")
    void getNullWhenGetObsoleteObjects(CacheService<CacheServiceTestObject> cacheService) throws InterruptedException {
        IntStream.range(0, 4)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });
        TimeUnit.SECONDS.sleep(4);
        IntStream.range(0, 2).forEach(x -> {
            String key = "key_" + x;
            CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
            cacheService.put(key, value);
        });
        TimeUnit.SECONDS.sleep(2);

        IntStream.range(0, 2)
                .forEach(x -> {
                    CacheServiceTestObject returnedValue = cacheService.get("key_" + x);
                    assertEquals("value_" + x, returnedValue.getField());
                });
        assertNull(cacheService.get("key_" + 2));
        assertNull(cacheService.get("key_" + 3));
    }

    @ParameterizedTest
    @DisplayName("Check cache clean up that removes obsolete objects after timeout time")
    @MethodSource("cacheServiceProvider")
    void checkCacheCleanUp(CacheService<CacheServiceTestObject> cacheService) throws InterruptedException {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });
        TimeUnit.SECONDS.sleep(6);
        cacheService.cacheCleanUp();

        IntStream.range(0, 100)
                .forEach(x -> assertNull(cacheService.get("key_" + x)));
    }

    @ParameterizedTest
    @DisplayName("Put 150 items to cache with size 100 getting least frequency objects removed, then wait 6 secs for removing obsolete objects." +
            "Then check cache statistics information about obsolete, evicted objects and average putting time.")
    @MethodSource("cacheServiceProvider")
    void checkStatisticsAfterRemovingLeastFrequencyAndObsoleteObjects(CacheService<CacheServiceTestObject> cacheService) throws InterruptedException {
        IntStream.range(0, 100)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });
        IntStream.range(100, 150)
                .forEach(x -> {
                    String key = "key_" + x;
                    CacheServiceTestObject value = new CacheServiceTestObject("value_" + x);
                    cacheService.put(key, value);
                });
        TimeUnit.SECONDS.sleep(6);
        cacheService.cacheCleanUp();

        CacheStatisticsObject cacheStatisticsObject = cacheService.returnCacheStatistics();

        String expectedCacheStatistics = "Statistics:\n" +
                "Eviction count = 150\n" +
                "Average load penalty = 0.0 ms\n";
        assertEquals(expectedCacheStatistics, cacheStatisticsObject.toString());
    }
}
