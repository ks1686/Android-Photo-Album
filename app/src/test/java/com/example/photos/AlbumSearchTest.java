package com.example.photos;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AlbumSearchTest {

    private Album album;
    private Photo johnNyc;
    private Photo johnLa;
    private Photo janeNyc;

    @Before
    public void setUp() {
        album = new Album("Vacation");
        johnNyc = new Photo("content://photos/john-nyc");
        johnNyc.addTag("person", "John");
        johnNyc.addTag("location", "New York");
        johnLa = new Photo("content://photos/john-la");
        johnLa.addTag("person", "John");
        johnLa.addTag("location", "LA");
        janeNyc = new Photo("content://photos/jane-nyc");
        janeNyc.addTag("person", "Jane");
        janeNyc.addTag("location", "New York");
        album.addPhoto(johnNyc);
        album.addPhoto(johnLa);
        album.addPhoto(janeNyc);
    }

    @Test
    public void constructorDoesNotRequireGlobalAlbumList() {
        Album created = new Album("Standalone");
        assertEquals("Standalone", created.getAlbumName());
        assertEquals(0, created.getSize());
    }

    @Test
    public void setAlbumNameRejectsEmptyButAllowsDuplicateNames() {
        Album other = new Album("Vacation");
        other.setAlbumName("Vacation");
        assertEquals("Vacation", other.getAlbumName());
        try {
            other.setAlbumName("");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void singleTagIsCaseInsensitivePrefixMatch() {
        List<Photo> results = album.search("PERSON=jo");
        assertEquals(2, results.size());
        assertTrue(results.contains(johnNyc));
        assertTrue(results.contains(johnLa));
    }

    @Test
    public void singleTagAllowsSpacesInValue() {
        List<Photo> results = album.search("location=New York");
        assertEquals(2, results.size());
        assertTrue(results.contains(johnNyc));
        assertTrue(results.contains(janeNyc));
    }

    @Test
    public void orQueryMatchesEitherTag() {
        List<Photo> results = album.search("person=Jane OR location=LA");
        assertEquals(2, results.size());
        assertTrue(results.contains(janeNyc));
        assertTrue(results.contains(johnLa));
    }

    @Test
    public void andQueryRequiresBothTags() {
        List<Photo> results = album.search("person=John AND location=New York");
        assertEquals(1, results.size());
        assertEquals(johnNyc, results.get(0));
    }

    @Test
    public void operatorsAreMatchedCaseInsensitively() {
        List<Photo> results = album.search("person=john and location=new york");
        assertEquals(1, results.size());
        assertEquals(johnNyc, results.get(0));
    }

    @Test
    public void moreThanOneOperatorIsInvalid() {
        try {
            album.search("person=John AND location=LA OR person=Jane");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void malformedQueryIsInvalid() {
        try {
            album.search("not-a-query");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
