package com.example.photos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Photo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String filepath;
    private final List<Tag> tags;

    public Photo(String filepath, List<Tag> tags) {
        if (filepath == null) {
            throw new NullPointerException("filepath cannot be null");
        }
        if (filepath.isEmpty()) {
            throw new IllegalArgumentException("filepath cannot be empty");
        }
        this.filepath = filepath;
        this.tags = new ArrayList<>();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public Photo(String filepath) {
        this(filepath, new ArrayList<>());
    }

    public Photo copy() {
        List<Tag> copiedTags = new ArrayList<>();
        for (Tag tag : tags) {
            copiedTags.add(new Tag(tag.getKey(), tag.getValue()));
        }
        return new Photo(filepath, copiedTags);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Photo)) {
            return false;
        }
        Photo photo = (Photo) obj;
        return filepath.equals(photo.filepath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filepath);
    }

    public void deleteTag(String key, String value) {
        for (int i = 0; i < tags.size(); i++) {
            Tag tag = tags.get(i);
            if (tag.getKey().equalsIgnoreCase(key) && tag.getValue().equals(value)) {
                tags.remove(i);
                return;
            }
        }
    }

    public String getFilePath() {
        return filepath;
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void addTag(String key, String value) {
        tags.add(new Tag(key, value));
    }

    public boolean hasTagMatch(String key, String valuePrefix) {
        for (Tag tag : tags) {
            if (tag.matches(key, valuePrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Photo: %s || Tags: %s", filepath, tags);
    }
}
