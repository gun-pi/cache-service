package com.cacheservice.simplejava.listener;

import com.cacheservice.simplejava.CachedObject;
import com.cacheservice.simplejava.EventType;

import java.util.EventListener;

public interface Listener extends EventListener {

    void onEvent(EventType eventType, CachedObject<?> cachedObject);

}
