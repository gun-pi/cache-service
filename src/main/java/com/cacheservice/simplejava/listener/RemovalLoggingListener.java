package com.cacheservice.simplejava.listener;

import com.cacheservice.simplejava.CachedObject;
import com.cacheservice.simplejava.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removal logging listener for logging removal events
 */
public class RemovalLoggingListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemovalLoggingListener.class.getName());

    /**
     * Listener action when event is happening. Logs removal events
     *
     * @param eventType    event type
     * @param cachedObject cached object
     */
    @Override
    public void onEvent(EventType eventType, CachedObject<?> cachedObject) {
        if (eventType == EventType.REMOVE_OBSOLETE_OBJECT) {
            LOGGER.trace("The obsolete object with key '{}' is deleted: last access > 5 secs.", cachedObject.getKey());
        } else if (eventType == EventType.REMOVE_LEAST_FREQUENCY_OBJECT) {
            LOGGER.trace("Cache size exceeded max size.\nThe least frequency object with key '{}' is deleted.", cachedObject.getKey());
        }
    }
}
