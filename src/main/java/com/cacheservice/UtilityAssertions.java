package com.cacheservice;

/**
 * Utility class with assertions for String fields and Object fields
 */
public final class UtilityAssertions {

    /**
     * Assertions for String fields
     *
     * @param field  first field
     * @param fields other fields
     */
    public static void assertInputStringsNotBlankOrNull(String field, String... fields) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Input string #1 is null or blank!");
        }
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null || fields[i].isBlank()) {
                int inputFiledIndex = i + 2;
                String msg = String.format("Input string #%d is null or blank!", inputFiledIndex);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * Assertions for Object fields
     *
     * @param field  first field
     * @param fields other fields
     */
    public static void assertInputObjectsNotNull(Object field, Object... fields) {
        if (field == null) {
            throw new IllegalArgumentException("Input object #1 is null!");
        }
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) {
                int inputFiledIndex = i + 2;
                String msg = String.format("Input object #%d is null!", inputFiledIndex);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    private UtilityAssertions() {
        throw new AssertionError("UtilityAssertions constructor should not be used!");
    }
}
