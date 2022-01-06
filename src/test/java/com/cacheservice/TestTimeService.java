package com.cacheservice;

import com.cacheservice.simplejava.TimeService;

public class TestTimeService extends TimeService {

    @Override
    public long getTimeWithSystemDefaultZoneEpochMilli() {
        return super.getUtcTimeEpochMilli();
    }
}
