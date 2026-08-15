package com.example.photos;

import java.io.Serializable;
import java.util.Objects;

/**
 * A photo tag. Allowed keys are {@code person} and {@code location}.
 */
public final class Tag implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PERSON = "person";
    public static final String LOCATION = "location";

    private final String key;
    private final String value;

    public Tag(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value cannot be empty");
        }
        String normalized = key.toLowerCase();
        if (!PERSON.equals(normalized) && !LOCATION.equals(normalized)) {
            throw new IllegalArgumentException("key must be either 'person' or 'location'");
        }
        this.key = normalized;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String queryKey, String queryValuePrefix) {
        if (queryKey == null || queryValuePrefix == null) {
            return false;
        }
        return key.equalsIgnoreCase(queryKey)
                && value.toLowerCase().startsWith(queryValuePrefix.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        Tag tag = (Tag) o;
        return key.equals(tag.key) && value.equals(tag.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }
}
