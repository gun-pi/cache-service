package com.cacheservice.simplejava;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Time service
 */
public class TimeService {

    /**
     * Returns UTC Epoch time in millis
     *
     * @return UTC Epoch time in millis
     */
    public long getUtcTimeEpochMilli() {
        return LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /**
     * Returns system default Epoch time in millis
     *
     * @return system default Epoch time in millis
     */
    public long getTimeWithSystemDefaultZoneEpochMilli() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
