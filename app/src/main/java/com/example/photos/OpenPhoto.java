package com.example.photos;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.photos.R;

import java.util.ArrayList;
import java.util.List;

public class OpenPhoto extends AppCompatActivity {

    public static final String PHOTO_FILEPATH = "photoFilepath";
    public static final String ALBUM_INDEX = "albumIndex";

    private String photoFilepath = "";
    private int albumIndex;
    private Album album;

    private ImageView photoView;
    private TextView tagsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_photo);

        Toolbar displayPhotoToolbar = findViewById(R.id.display_photo_toolbar);
        displayPhotoToolbar.setTitle("");
        setSupportActionBar(displayPhotoToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        displayPhotoToolbar.setNavigationOnClickListener(view -> finish());

        photoView = findViewById(R.id.photo_view);
        tagsTextView = findViewById(R.id.tags_textView);

        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle bundle = getIntent().getExtras();
            photoFilepath = bundle.getString(PHOTO_FILEPATH, "");
            albumIndex = bundle.getInt(ALBUM_INDEX, -1);
        }

        album = Photos.albumAt(albumIndex);
        if (album == null || album.findByFilePath(photoFilepath) == null) {
            Toast.makeText(this, R.string.photo_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.prev_photo_button).setOnClickListener(view -> showAdjacent(-1));
        findViewById(R.id.next_photo_button).setOnClickListener(view -> showAdjacent(1));
        findViewById(R.id.add_tag).setOnClickListener(view -> addTag());
        findViewById(R.id.remove_tag).setOnClickListener(view -> removeTag());
        findViewById(R.id.move_button).setOnClickListener(view -> movePhoto());
        findViewById(R.id.delete_button).setOnClickListener(view -> deletePhoto());
        findViewById(R.id.back_button).setOnClickListener(view -> finish());

        bindCurrentPhoto();
    }

    private Photo currentPhoto() {
        album = Photos.albumAt(albumIndex);
        if (album == null) {
            return null;
        }
        return album.findByFilePath(photoFilepath);
    }

    private void bindCurrentPhoto() {
        Photo photo = currentPhoto();
        if (photo == null) {
            finish();
            return;
        }
        Glide.with(this)
                .load(Uri.parse(photo.getFilePath()))
                .fitCenter()
                .into(photoView);
        photoView.setContentDescription(getString(R.string.current_photo_content_description));
        setTagsText(photo);
    }

    public void setTagsText(Photo photo) {
        if (photo == null) {
            tagsTextView.setText("");
            return;
        }
        StringBuilder tagsString = new StringBuilder();
        for (Tag tag : photo.getTags()) {
            tagsString.append(tag.getKey()).append(": ").append(tag.getValue()).append('\n');
        }
        tagsTextView.setText(tagsString.toString());
    }

    public void removeTag() {
        Photo photo = currentPhoto();
        if (photo == null) {
            return;
        }
        List<Tag> tags = photo.getTags();
        if (tags.isEmpty()) {
            Toast.makeText(this, R.string.no_tags, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagStrings = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            tagStrings[i] = tags.get(i).toString();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_tag_title)
                .setItems(tagStrings, (dialog, which) -> {
                    Tag tag = tags.get(which);
                    photo.deleteTag(tag.getKey(), tag.getValue());
                    setTagsText(photo);
                    Photos.saveAlbumsToFile(this);
                })
                .show();
    }

    public void addTag() {
        String[] keys = {Tag.PERSON, Tag.LOCATION};
        final int[] checkedItem = {0};
        EditText valueEditText = new EditText(this);
        valueEditText.setHint(R.string.tag_value_hint);
        valueEditText.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_tag_title)
                .setSingleChoiceItems(keys, 0, (dialog, which) -> checkedItem[0] = which)
                .setView(valueEditText)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    Photo photo = currentPhoto();
                    if (photo == null) {
                        return;
                    }
                    try {
                        photo.addTag(keys[checkedItem[0]], valueEditText.getText().toString().trim());
                        setTagsText(photo);
                        Photos.saveAlbumsToFile(this);
                    } catch (RuntimeException e) {
                        Toast.makeText(this, R.string.tag_invalid, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void showAdjacent(int delta) {
        Photo photo = currentPhoto();
        if (photo == null) {
            return;
        }
        int index = album.indexOfFilePath(photoFilepath);
        int next = index + delta;
        if (index < 0 || next < 0 || next >= album.getSize()) {
            Toast.makeText(this, R.string.no_more_photos, Toast.LENGTH_SHORT).show();
            return;
        }
        photoFilepath = album.getPhotos().get(next).getFilePath();
        bindCurrentPhoto();
    }

    public void movePhoto() {
        Photo photo = currentPhoto();
        if (photo == null) {
            return;
        }
        List<Album> destinations = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (Photos.albums != null) {
            for (Album candidate : Photos.albums) {
                if (candidate.isTempAlbum) {
                    continue;
                }
                names.add(candidate.getAlbumName());
                destinations.add(candidate);
            }
        }
        if (destinations.isEmpty()) {
            Toast.makeText(this, R.string.no_albums, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.move_photo_title)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    Album target = destinations.get(which);
                    Album source = sourceAlbumFor(photo);
                    if (source == target) {
                        Toast.makeText(this, R.string.photo_already_in_album, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        target.addPhoto(photo.copy());
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, R.string.photo_already_in_album, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (source != null) {
                        source.removePhoto(photo);
                    }
                    if (album.isTempAlbum) {
                        album.removePhoto(photo);
                    }
                    Photos.saveAlbumsToFile(this);
                    finish();
                })
                .show();
    }

    private Album sourceAlbumFor(Photo photo) {
        if (album != null && !album.isTempAlbum) {
            return album;
        }
        if (Photos.albums == null) {
            return null;
        }
        for (Album candidate : Photos.albums) {
            if (!candidate.isTempAlbum && candidate.findByFilePath(photo.getFilePath()) != null) {
                return candidate;
            }
        }
        return null;
    }

    public void deletePhoto() {
        Photo photo = currentPhoto();
        if (photo == null) {
            return;
        }
        Album source = sourceAlbumFor(photo);
        if (source != null) {
            source.removePhoto(photo);
        }
        if (album != null && album.isTempAlbum) {
            album.removePhoto(photo);
        }
        Photos.saveAlbumsToFile(this);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Photos.saveAlbumsToFile(this);
    }
}
