package com.example.photos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.photos.R;

import java.util.ArrayList;
import java.util.List;

public class OpenAlbum extends AppCompatActivity {

    public static final String ALBUM_NAME = "albumName";
    public static final String ALBUM_INDEX = "albumIndex";
    public static final int DELETE_ALBUM_RESULT = 2;

    private int albumIndex;
    private boolean searchMode;
    private Album album;
    private EditText albumName;
    private ImageAdapter imageAdapter;
    private final ActivityResultLauncher<String[]> pickImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, @NonNull String[] input) {
                    Intent intent = super.createIntent(context, input);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    return intent;
                }
            },
            uri -> {
                if (uri == null || album == null) {
                    return;
                }
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // persistable grant is best-effort; the current session can still read the URI
                }
                try {
                    album.addPhoto(new Photo(uri.toString()));
                    imageAdapter.updatePhotos(album.getPhotos());
                    Photos.saveAlbumsToFile(this);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, R.string.photo_already_in_album, Toast.LENGTH_SHORT).show();
                }
            });

    public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private final List<Photo> photos = new ArrayList<>();

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Photo photo = photos.get(position);
            Glide.with(holder.imageView)
                    .load(Uri.parse(photo.getFilePath()))
                    .centerCrop()
                    .into(holder.imageView);
            holder.imageView.setContentDescription(getString(R.string.photo_content_description, position + 1));
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        public void updatePhotos(List<Photo> newPhotos) {
            photos.clear();
            if (newPhotos != null) {
                photos.addAll(newPhotos);
            }
            notifyDataSetChanged();
        }

        public class ImageViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_view);
                imageView.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return;
                    }
                    Photo photo = photos.get(position);
                    Intent intent = new Intent(OpenAlbum.this, OpenPhoto.class);
                    intent.putExtra(OpenPhoto.ALBUM_INDEX, albumIndex);
                    intent.putExtra(OpenPhoto.PHOTO_FILEPATH, photo.getFilePath());
                    startActivity(intent);
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_album);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, R.string.album_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        albumIndex = extras.getInt(ALBUM_INDEX, -1);
        searchMode = albumIndex == Photos.SEARCH_RESULTS_INDEX;
        album = Photos.albumAt(albumIndex);
        if (album == null) {
            Toast.makeText(this, R.string.album_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        myToolbar.setNavigationOnClickListener(view -> finish());

        albumName = findViewById(R.id.album_name);
        String extraName = extras.getString(ALBUM_NAME, album.getAlbumName());
        albumName.setText(extraName);

        Button deleteAlbumButton = findViewById(R.id.delete_album_button);
        Button addPhotoButton = findViewById(R.id.add_photo_button);
        Button renameAlbumButton = findViewById(R.id.rename_album_button);
        deleteAlbumButton.setOnClickListener(view -> deleteAlbum());
        addPhotoButton.setOnClickListener(view -> {
            pickImage.launch(new String[]{"image/*"});
        });
        renameAlbumButton.setOnClickListener(view -> renameAlbum());

        if (searchMode) {
            deleteAlbumButton.setVisibility(View.GONE);
            addPhotoButton.setVisibility(View.GONE);
            renameAlbumButton.setVisibility(View.GONE);
            albumName.setEnabled(false);
        }

        RecyclerView imageListView = findViewById(R.id.image_list_view);
        imageListView.setLayoutManager(new GridLayoutManager(this, 3));
        imageAdapter = new ImageAdapter();
        imageListView.setAdapter(imageAdapter);
        imageAdapter.updatePhotos(album.getPhotos());
    }

    @Override
    protected void onResume() {
        super.onResume();
        album = Photos.albumAt(albumIndex);
        if (album == null || imageAdapter == null) {
            finish();
            return;
        }
        imageAdapter.updatePhotos(album.getPhotos());
        if (albumName != null) {
            albumName.setText(album.getAlbumName());
        }
    }

    public void deleteAlbum() {
        if (searchMode) {
            finish();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(ALBUM_INDEX, albumIndex);
        setResult(DELETE_ALBUM_RESULT, intent);
        finish();
    }

    public void renameAlbum() {
        if (searchMode) {
            return;
        }
        String albumNameString = albumName.getText().toString().trim();
        try {
            if (albumNameString.isEmpty()) {
                Toast.makeText(this, R.string.album_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (AlbumStore.albumNameExists(Photos.albums, albumNameString, album)) {
                Toast.makeText(this, R.string.album_name_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            album.setAlbumName(albumNameString);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, R.string.album_name_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        Photos.saveAlbumsToFile(this);
        Intent result = new Intent();
        result.putExtra(ALBUM_INDEX, albumIndex);
        result.putExtra(ALBUM_NAME, album.getAlbumName());
        setResult(RESULT_OK, result);
        Toast.makeText(this, R.string.album_renamed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
