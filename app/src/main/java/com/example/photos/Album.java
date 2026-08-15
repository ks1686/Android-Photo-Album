package com.example.photos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An album of photos. Name uniqueness is enforced by callers, not this class.
 */
public class Album implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Pattern OPERATOR = Pattern.compile("(?i)\\s+(AND|OR)\\s+");
    private static final Pattern TAG_PAIR = Pattern.compile("^\\s*([^=]+?)=(.*\\S)\\s*$");

    private String albumName;
    private final List<Photo> photos;
    public boolean isTempAlbum = false;

    /**
     * Creates an album with the given name and an empty list of photos.
     *
     * @param albumName the name of the album
     * @throws NullPointerException     if albumName is null
     * @throws IllegalArgumentException if albumName is empty
     */
    public Album(String albumName) throws NullPointerException, IllegalArgumentException {
        this(albumName, new ArrayList<>());
    }

    /**
     * Creates an album with the given name and list of photos.
     *
     * @param albumName the name of the album
     * @param photos    the list of photos in the album
     * @throws NullPointerException     if albumName or photos is null
     * @throws IllegalArgumentException if albumName is empty
     */
    public Album(String albumName, List<Photo> photos) {
        if (photos == null) {
            throw new NullPointerException("photos cannot be null");
        }
        setAlbumName(albumName);
        this.photos = new ArrayList<>(photos);
        this.isTempAlbum = false;
    }

    /**
     * Adds a photo to the album.
     *
     * @param photo the photo to add
     */
    public void addPhoto(Photo photo) throws NullPointerException, IllegalArgumentException {
        // check if photo is null or photo is already in the album. throw error
        if (photo == null) {
            throw new NullPointerException("photo cannot be null");
        }

        if (this.photos.contains(photo)) {
            throw new IllegalArgumentException("Photo already exists in the album");
        }

        this.photos.add(photo); // may need to catch an exception here?
    }

    /**
     * Adds a photo to the album.
     *
     * @param filepath the filepath of the photo to add
     */
    public void addPhoto(String filepath) {
        addPhoto(new Photo(filepath));
    }

    /**
     * Removes a photo from the album.
     *
     * @param photo the photo to remove
     */
    public void removePhoto(Photo photo) {
        this.photos.remove(photo);
    }

    /**
     * Gets a photo from the album.
     *
     * @return arrayList of photos
     */
    public List<Photo> getPhotos() {
        return Collections.unmodifiableList(this.photos);
    }

    public Photo findByFilePath(String filepath) {
        int index = indexOfFilePath(filepath);
        if (index < 0) {
            return null;
        }
        return photos.get(index);
    }

    public int indexOfFilePath(String filepath) {
        if (filepath == null) {
            return -1;
        }
        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getFilePath().equals(filepath)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * get the name of the album
     *
     * @return the name of the album
     */
    public String getAlbumName() {
        return albumName;
    }

    /**f
     * set the name of the album
     *
     * @param albumName the name of the album
     * @throws NullPointerException     if albumName is null
     * @throws IllegalArgumentException if albumName is empty
     */
    public void setAlbumName(String albumName) {
        if (albumName == null) {
            throw new NullPointerException("albumName cannot be null");
        }
        if (albumName.isEmpty()) {
            throw new IllegalArgumentException("albumName cannot be empty");
        }
        this.albumName = albumName;
    }

    /**
     * get the size of the album
     *
     * @return the size of the album
     */
    public int getSize() {
        return this.photos.size();
    }

    /**
     * toString method for the album
     *
     * @return the string representation of the album
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Photo photo : this.photos) {
            result.append(photo).append('\n');
        }
        return "Album: " + this.albumName + "\nPhotos:\n" + result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Album)) {
            return false;
        }
        Album album = (Album) o;
        return album.getAlbumName().equals(this.albumName) && album.photos.equals(this.photos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(albumName, photos);
    }

    /**
     * search for photos in the album based on a query
     *
     * @param query the query to search for
     * @return the list of photos that match the query
     * @throws IllegalArgumentException if the query is invalid
     */
    public List<Photo> search(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query");
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Invalid query");
        }

        Matcher operatorMatcher = OPERATOR.matcher(trimmed);
        String operator = null;
        int operatorCount = 0;
        int firstStart = -1;
        int firstEnd = -1;
        while (operatorMatcher.find()) {
            operatorCount++;
            if (operatorCount == 1) {
                operator = operatorMatcher.group(1).toUpperCase(Locale.ROOT);
                firstStart = operatorMatcher.start();
                firstEnd = operatorMatcher.end();
            }
        }
        if (operatorCount > 1) {
            throw new IllegalArgumentException("Invalid query");
        }

        if (operatorCount == 0) {
            String[] pair = parseTagPair(trimmed);
            return collectMatches(pair[0], pair[1], null, null, false);
        }

        String left = trimmed.substring(0, firstStart);
        String right = trimmed.substring(firstEnd);
        String[] first = parseTagPair(left);
        String[] second = parseTagPair(right);
        boolean requireBoth = "AND".equals(operator);
        return collectMatches(first[0], first[1], second[0], second[1], requireBoth);
    }

    private static String[] parseTagPair(String raw) {
        Matcher matcher = TAG_PAIR.matcher(raw);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query");
        }
        String key = matcher.group(1).trim();
        String value = matcher.group(2).trim();
        if (key.isEmpty() || value.isEmpty() || key.contains("=")) {
            throw new IllegalArgumentException("Invalid query");
        }
        return new String[]{key, value};
    }

    private List<Photo> collectMatches(
            String key1,
            String value1,
            String key2,
            String value2,
            boolean requireBoth
    ) {
        List<Photo> result = new ArrayList<>();
        for (Photo photo : this.photos) {
            boolean first = photo.hasTagMatch(key1, value1);
            if (key2 == null) {
                if (first) {
                    result.add(photo);
                }
                continue;
            }
            boolean second = photo.hasTagMatch(key2, value2);
            if (requireBoth ? (first && second) : (first || second)) {
                result.add(photo);
            }
        }
        return result;
    }

}