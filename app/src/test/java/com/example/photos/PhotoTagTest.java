package com.example.photos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PhotoTagTest {

    @Test
    public void addTagStoresTypedTag() {
        Photo photo = new Photo("content://photos/a");
        photo.addTag("person", "Ada");
        assertEquals(1, photo.getTags().size());
        Tag tag = photo.getTags().get(0);
        assertEquals("person", tag.getKey());
        assertEquals("Ada", tag.getValue());
    }

    @Test
    public void addTagRejectsUnknownKey() {
        Photo photo = new Photo("content://photos/a");
        try {
            photo.addTag("camera", "Nikon");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void deleteTagRemovesMatchingPair() {
        Photo photo = new Photo("content://photos/a");
        photo.addTag("person", "Ada");
        photo.addTag("location", "Paris");
        photo.deleteTag("person", "Ada");
        assertEquals(1, photo.getTags().size());
        assertEquals("location", photo.getTags().get(0).getKey());
    }

    @Test
    public void copyPreservesPathAndTags() {
        Photo photo = new Photo("content://photos/a");
        photo.addTag("person", "Ada");
        Photo copy = photo.copy();
        assertEquals(photo, copy);
        assertEquals(1, copy.getTags().size());
        assertEquals("Ada", copy.getTags().get(0).getValue());
        copy.addTag("location", "Paris");
        assertEquals(1, photo.getTags().size());
    }

    @Test
    public void equalsAndHashCodeUseFilePath() {
        Photo a = new Photo("content://photos/a");
        Photo b = new Photo("content://photos/a");
        Photo c = new Photo("content://photos/b");
        a.addTag("person", "Ada");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void albumRejectsDuplicatePhoto() {
        Album album = new Album("Dupes");
        album.addPhoto(new Photo("content://photos/a"));
        try {
            album.addPhoto(new Photo("content://photos/a"));
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void findByFilePathAndRemove() {
        Album album = new Album("Find");
        Photo photo = new Photo("content://photos/a");
        album.addPhoto(photo);
        assertEquals(photo, album.findByFilePath("content://photos/a"));
        album.removePhoto(album.findByFilePath("content://photos/a"));
        assertEquals(0, album.getSize());
        assertTrue(album.getPhotos().isEmpty());
    }

    @Test
    public void indexOfFilePath() {
        Album album = new Album("Index");
        album.addPhoto(new Photo("content://photos/a"));
        album.addPhoto(new Photo("content://photos/b"));
        assertEquals(1, album.indexOfFilePath("content://photos/b"));
        assertEquals(-1, album.indexOfFilePath("missing"));
    }
}
