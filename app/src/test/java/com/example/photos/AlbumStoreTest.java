package com.example.photos;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlbumStoreTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void saveAndLoadRoundTripsAlbumsPhotosAndTags() throws Exception {
        File file = folder.newFile("albums.json");
        AlbumStore store = new AlbumStore(file);

        Album vacation = new Album("Vacation");
        Photo photo = new Photo("content://photos/a");
        photo.addTag("person", "Ada");
        photo.addTag("location", "Paris");
        vacation.addPhoto(photo);

        Album work = new Album("Work");
        work.addPhoto(new Photo("content://photos/b"));

        List<Album> albums = new ArrayList<>();
        albums.add(vacation);
        albums.add(work);
        store.save(albums);

        String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(raw.contains("Vacation"));
        assertTrue(raw.contains("Ada"));

        List<Album> loaded = store.load();
        assertEquals(2, loaded.size());
        assertEquals("Vacation", loaded.get(0).getAlbumName());
        assertEquals(1, loaded.get(0).getSize());
        Photo loadedPhoto = loaded.get(0).getPhotos().get(0);
        assertEquals("content://photos/a", loadedPhoto.getFilePath());
        assertEquals(2, loadedPhoto.getTags().size());
        assertEquals("person", loadedPhoto.getTags().get(0).getKey());
        assertEquals("Ada", loadedPhoto.getTags().get(0).getValue());
        assertEquals("Work", loaded.get(1).getAlbumName());
    }

    @Test
    public void saveSkipsTemporaryAlbums() throws Exception {
        File file = folder.newFile("albums.json");
        AlbumStore store = new AlbumStore(file);

        Album real = new Album("Real");
        Album temp = new Album("Search Results");
        temp.isTempAlbum = true;
        temp.addPhoto(new Photo("content://photos/temp"));

        List<Album> albums = new ArrayList<>();
        albums.add(real);
        albums.add(temp);
        store.save(albums);

        List<Album> loaded = store.load();
        assertEquals(1, loaded.size());
        assertEquals("Real", loaded.get(0).getAlbumName());
        assertFalse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
                .contains("Search Results"));
    }

    @Test
    public void loadMissingFileReturnsEmptyList() {
        File file = new File(folder.getRoot(), "missing.json");
        AlbumStore store = new AlbumStore(file);
        assertTrue(store.load().isEmpty());
    }

    @Test
    public void searchAllSkipsTempAlbumsAndDuplicatePaths() {
        Album first = new Album("A");
        Photo shared = new Photo("content://photos/shared");
        shared.addTag("person", "Ada");
        first.addPhoto(shared);

        Album second = new Album("B");
        Photo copy = new Photo("content://photos/shared");
        copy.addTag("person", "Ada");
        second.addPhoto(copy);
        Photo other = new Photo("content://photos/other");
        other.addTag("person", "Ada");
        second.addPhoto(other);

        Album temp = new Album("Search Results");
        temp.isTempAlbum = true;
        Photo tempPhoto = new Photo("content://photos/temp");
        tempPhoto.addTag("person", "Ada");
        temp.addPhoto(tempPhoto);

        List<Album> albums = new ArrayList<>();
        albums.add(first);
        albums.add(second);
        albums.add(temp);

        Album results = AlbumStore.searchAll(albums, "person=Ada");
        assertTrue(results.isTempAlbum);
        assertEquals(2, results.getSize());
        assertEquals("content://photos/shared", results.getPhotos().get(0).getFilePath());
        assertEquals("content://photos/other", results.getPhotos().get(1).getFilePath());
    }

    @Test
    public void albumNameExistsIsCaseSensitive() {
        List<Album> albums = new ArrayList<>();
        albums.add(new Album("Vacation"));
        assertTrue(AlbumStore.albumNameExists(albums, "Vacation", null));
        assertFalse(AlbumStore.albumNameExists(albums, "vacation", null));
        assertFalse(AlbumStore.albumNameExists(albums, "Vacation", albums.get(0)));
    }
}
