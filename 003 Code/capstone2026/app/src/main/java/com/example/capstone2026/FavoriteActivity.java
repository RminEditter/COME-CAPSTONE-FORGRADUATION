package com.example.capstone2026;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private FavoriteAdapter adapter;
    private List<FavoriteCafe> favoriteList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        rvFavorites = findViewById(R.id.rvFavorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));

        loadFavorites();

        adapter = new FavoriteAdapter(favoriteList);
        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        SharedPreferences prefs = getSharedPreferences("CafeFitFavorites", MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        for (String key : all.keySet()) {
            Object value = all.get(key);

            if (value instanceof Boolean && (Boolean) value) {
                String id = key;
                String name = prefs.getString(id + "_name", "이름 없음");
                String address = prefs.getString(id + "_address", "주소 없음");
                String tags = prefs.getString(id + "_tags", "");
                String reason = prefs.getString(id + "_reason", "");

                favoriteList.add(new FavoriteCafe(id, name, address, tags, reason));
            }
        }
    }
}