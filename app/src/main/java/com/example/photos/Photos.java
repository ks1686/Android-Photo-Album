package com.example.photos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.photos.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Photos extends AppCompatActivity {

    public static List<Album> albums;
    public static Album pendingSearchResults;
    public static final int SEARCH_RESULTS_INDEX = -2;
    private ListView listView;
    private ArrayAdapter<String> albumAdapter;
    private AlbumStore albumStore;
    private final ActivityResultLauncher<Intent> startForAlbumOpen = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::applyAlbumEdit);

    public List<String> getAlbumNames() {
        List<String> albumNames = new ArrayList<>();
        if (Photos.albums == null) {
            return albumNames;
        }
        for (Album album : Photos.albums) {
            if (!album.isTempAlbum) {
                albumNames.add(album.getAlbumName());
            }
        }
        return albumNames;
    }

    public static void saveAlbumsToFile(Context context) {
        if (context == null) {
            return;
        }
        try {
            AlbumStore.fromAppFilesDir(context.getFilesDir()).save(Photos.albums);
        } catch (IOException e) {
            // Persistence failure is non-fatal for the current session.
        }
    }

    private void ensureAlbumsLoaded() {
        if (Photos.albums == null) {
            Photos.albums = albumStore.load();
        }
        removeTempAlbums();
    }

    private void removeTempAlbums() {
        if (Photos.albums == null) {
            Photos.albums = new ArrayList<>();
            return;
        }
        Iterator<Album> iterator = Photos.albums.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isTempAlbum) {
                iterator.remove();
            }
        }
    }

    private void refreshAlbumList() {
        if (albumAdapter == null) {
            albumAdapter = new ArrayAdapter<>(this, R.layout.album, getAlbumNames());
            listView.setAdapter(albumAdapter);
        } else {
            albumAdapter.clear();
            albumAdapter.addAll(getAlbumNames());
            albumAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albums_list);

        albumStore = AlbumStore.fromAppFilesDir(getFilesDir());
        ensureAlbumsLoaded();

        Toolbar myToolbar = findViewById(R.id.albums_toolbar);
        setSupportActionBar(myToolbar);

        FloatingActionButton createAlbumButton = findViewById(R.id.create_album_button);
        createAlbumButton.setOnClickListener(view -> createAlbum());

        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setHint(R.string.search_hint);
        searchBar.setText("");
        searchBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            boolean enter = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && !event.isShiftPressed();
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || enter) {
                runSearch(searchBar.getText().toString());
                return true;
            }
            return false;
        });

        listView = findViewById(R.id.albums_list);
        refreshAlbumList();
        listView.setOnItemClickListener((list, view, pos, id) -> showAlbum(pos));
    }

    private void runSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        try {
            Album searchResults = AlbumStore.searchAll(Photos.albums, query);
            if (searchResults.getSize() == 0) {
                Toast.makeText(this, R.string.search_no_results, Toast.LENGTH_SHORT).show();
                return;
            }
            Photos.pendingSearchResults = searchResults;
            Intent intent = new Intent(this, OpenAlbum.class);
            intent.putExtra(OpenAlbum.ALBUM_INDEX, Photos.SEARCH_RESULTS_INDEX);
            intent.putExtra(OpenAlbum.ALBUM_NAME, searchResults.getAlbumName());
            startForAlbumOpen.launch(intent);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, R.string.search_invalid_query, Toast.LENGTH_SHORT).show();
        }
    }

    public static Album albumAt(int index) {
        if (index == SEARCH_RESULTS_INDEX) {
            return pendingSearchResults;
        }
        if (albums == null || index < 0 || index >= albums.size()) {
            return null;
        }
        return albums.get(index);
    }

    private void applyAlbumEdit(ActivityResult result) {
        Photos.pendingSearchResults = null;
        ensureAlbumsLoaded();
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            int albumIndex = result.getData().getIntExtra(OpenAlbum.ALBUM_INDEX, -1);
            String albumName = result.getData().getStringExtra(OpenAlbum.ALBUM_NAME);
            if (albumIndex >= 0 && albumIndex < Photos.albums.size() && albumName != null) {
                Photos.albums.get(albumIndex).setAlbumName(albumName);
            }
        } else if (result.getResultCode() == OpenAlbum.DELETE_ALBUM_RESULT && result.getData() != null) {
            int albumIndex = result.getData().getIntExtra(OpenAlbum.ALBUM_INDEX, -1);
            if (albumIndex >= 0 && albumIndex < Photos.albums.size()) {
                Photos.albums.remove(albumIndex);
            }
        }
        refreshAlbumList();
        saveAlbumsToFile(this);
    }

    private void showAlbum(int pos) {
        List<Integer> realIndexes = new ArrayList<>();
        for (int i = 0; i < Photos.albums.size(); i++) {
            if (!Photos.albums.get(i).isTempAlbum) {
                realIndexes.add(i);
            }
        }
        if (pos < 0 || pos >= realIndexes.size()) {
            return;
        }
        int albumIndex = realIndexes.get(pos);
        Intent intent = new Intent(this, OpenAlbum.class);
        intent.putExtra(OpenAlbum.ALBUM_INDEX, albumIndex);
        intent.putExtra(OpenAlbum.ALBUM_NAME, Photos.albums.get(albumIndex).getAlbumName());
        startForAlbumOpen.launch(intent);
    }

    private void createAlbum() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_album_title);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String albumName = input.getText().toString().trim();
            if (albumName.isEmpty()) {
                Toast.makeText(this, R.string.album_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            if (AlbumStore.albumNameExists(Photos.albums, albumName, null)) {
                Toast.makeText(this, R.string.album_name_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            Photos.albums.add(new Album(albumName));
            refreshAlbumList();
            saveAlbumsToFile(this);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            createAlbum();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureAlbumsLoaded();
        refreshAlbumList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveAlbumsToFile(this);
    }
}
