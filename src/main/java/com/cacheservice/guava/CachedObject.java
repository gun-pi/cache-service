package com.cacheservice.guava;

public class CachedObject<T> {

    private final T value;

    public CachedObject(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
