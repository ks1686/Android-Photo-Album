package com.example.photos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON persistence for albums. Temporary albums are never written.
 */
public class AlbumStore {
    public static final String FILE_NAME = "albums.json";

    private final File file;

    public AlbumStore(File file) {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }
        this.file = file;
    }

    public static AlbumStore fromAppFilesDir(File filesDir) {
        return new AlbumStore(new File(filesDir, FILE_NAME));
    }

    public static Album searchAll(List<Album> albums, String query) {
        Album results = new Album("Search Results");
        results.isTempAlbum = true;
        if (albums == null) {
            return results;
        }
        for (Album album : albums) {
            if (album == null || album.isTempAlbum) {
                continue;
            }
            for (Photo photo : album.search(query)) {
                if (results.findByFilePath(photo.getFilePath()) == null) {
                    results.addPhoto(photo);
                }
            }
        }
        return results;
    }

    public static boolean albumNameExists(List<Album> albums, String name, Album ignore) {
        if (albums == null || name == null) {
            return false;
        }
        for (Album album : albums) {
            if (album == ignore) {
                continue;
            }
            if (name.equals(album.getAlbumName())) {
                return true;
            }
        }
        return false;
    }

    public void save(List<Album> albums) throws IOException {
        JSONArray jsonArray = new JSONArray();
        if (albums != null) {
            for (Album album : albums) {
                if (album == null || album.isTempAlbum) {
                    continue;
                }
                try {
                    jsonArray.put(albumToJson(album));
                } catch (JSONException e) {
                    throw new IOException("Failed to serialize albums", e);
                }
            }
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory: " + parent);
        }

        File temp = new File(file.getAbsolutePath() + ".tmp");
        byte[] bytes = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(bytes);
            fos.flush();
        }
        if (file.exists() && !file.delete()) {
            // fall through to rename; delete failure is not fatal if rename replaces
        }
        if (!temp.renameTo(file)) {
            // last-resort copy if rename failed across filesystems
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
            if (!temp.delete()) {
                // leftover temp is harmless
            }
        }
    }

    public List<Album> load() {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        String raw;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int read = 0;
            while (read < buffer.length) {
                int n = fis.read(buffer, read, buffer.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            raw = new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ArrayList<>();
        }
        if (raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return parseAlbums(new JSONArray(raw));
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    private static JSONObject albumToJson(Album album) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("albumName", album.getAlbumName());
        JSONArray photosJsonArray = new JSONArray();
        for (Photo photo : album.getPhotos()) {
            JSONObject photoJsonObject = new JSONObject();
            photoJsonObject.put("name", photo.getFilePath());
            JSONArray tagsJsonArray = new JSONArray();
            for (Tag tag : photo.getTags()) {
                JSONObject tagJsonObject = new JSONObject();
                tagJsonObject.put("key", tag.getKey());
                tagJsonObject.put("value", tag.getValue());
                tagsJsonArray.put(tagJsonObject);
            }
            photoJsonObject.put("tags", tagsJsonArray);
            photosJsonArray.put(photoJsonObject);
        }
        jsonObject.put("photos", photosJsonArray);
        return jsonObject;
    }

    private static List<Album> parseAlbums(JSONArray jsonArray) throws JSONException {
        List<Album> albums = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Album album = new Album(jsonObject.getString("albumName"));
            JSONArray photosJsonArray = jsonObject.getJSONArray("photos");
            for (int j = 0; j < photosJsonArray.length(); j++) {
                JSONObject photoJsonObject = photosJsonArray.getJSONObject(j);
                Photo photo = new Photo(photoJsonObject.getString("name"));
                if (photoJsonObject.has("tags")) {
                    JSONArray tagsJsonArray = photoJsonObject.getJSONArray("tags");
                    for (int k = 0; k < tagsJsonArray.length(); k++) {
                        JSONObject tagJsonObject = tagsJsonArray.getJSONObject(k);
                        photo.addTag(tagJsonObject.getString("key"), tagJsonObject.getString("value"));
                    }
                }
                album.addPhoto(photo);
            }
            albums.add(album);
        }
        return albums;
    }
}
