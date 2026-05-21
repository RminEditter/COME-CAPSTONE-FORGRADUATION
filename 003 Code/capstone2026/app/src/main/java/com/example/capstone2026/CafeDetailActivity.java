package com.example.capstone2026;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CafeDetailActivity extends AppCompatActivity {

    private TextView txtCafeName, txtAddress, txtTags, txtReason;
    private Button btnFavorite;

    private String cafeId;
    private String cafeName;

    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_detail);

        txtCafeName = findViewById(R.id.txtDetailCafeName);
        txtAddress = findViewById(R.id.txtDetailAddress);
        txtTags = findViewById(R.id.txtDetailTags);
        txtReason = findViewById(R.id.txtDetailReason);
        btnFavorite = findViewById(R.id.btnFavorite);

        cafeId = getIntent().getStringExtra("cafe_id");
        cafeName = getIntent().getStringExtra("cafe_name");

        String address = getIntent().getStringExtra("cafe_address");
        String tags = getIntent().getStringExtra("cafe_tags");
        String reason = getIntent().getStringExtra("cafe_reason");

        txtCafeName.setText(cafeName);
        txtAddress.setText(address);
        txtTags.setText(tags);
        txtReason.setText(reason);

        loadFavoriteState();

        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void loadFavoriteState() {
        SharedPreferences prefs =
                getSharedPreferences("CafeFitFavorites", MODE_PRIVATE);

        isFavorite = prefs.getBoolean(cafeId, false);

        updateFavoriteButton();
    }

    private void toggleFavorite() {

        isFavorite = !isFavorite;

        String address = getIntent().getStringExtra("cafe_address");
        String tags = getIntent().getStringExtra("cafe_tags");
        String reason = getIntent().getStringExtra("cafe_reason");

        SharedPreferences prefs =
                getSharedPreferences("CafeFitFavorites", MODE_PRIVATE);

        prefs.edit()
                .putBoolean(cafeId, isFavorite)
                .putString(cafeId + "_name", cafeName)
                .putString(cafeId + "_address", address)
                .putString(cafeId + "_tags", tags)
                .putString(cafeId + "_reason", reason)
                .apply();

        updateFavoriteButton();

        if (isFavorite) {
            Toast.makeText(this,
                    "즐겨찾기에 추가했어요.",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    "즐겨찾기에서 해제했어요.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFavoriteButton() {

        if (isFavorite) {
            btnFavorite.setText("♥ 즐겨찾기 해제");
        } else {
            btnFavorite.setText("♡ 즐겨찾기 추가");
        }
    }
}