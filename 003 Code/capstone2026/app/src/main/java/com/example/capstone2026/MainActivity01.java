package com.example.capstone2026;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MainActivity01 extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CafeAdapter adapter;
    private EditText etSearch;
    private Button btnSearch, btnFilter;

    private List<Recommender.CafeModel> allCafes = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main01);

        // 1. 뷰 연결 (XML ID와 정확히 일치시킴)
        recyclerView = findViewById(R.id.rv_cafe);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnFilter = findViewById(R.id.btn_filter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. 설문 데이터 받아오기
        if (getIntent().hasExtra("selected_tags")) {
            List<String> tagsFromSurvey = getIntent().getStringArrayListExtra("selected_tags");
            if (tagsFromSurvey != null) {
                for (String s : tagsFromSurvey) {
                    try { selectedTags.add(Tag.valueOf(s)); }
                    catch (Exception e) { Log.e("TAG", "잘못된 태그: " + s); }
                }
            }
        }

        // 3. 버튼 클릭 리스너 설정
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            performSearch(query);
        });

        btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity01.this, SurveyActivity01.class);
            startActivity(intent);
            finish();
        });

        // 4. 데이터 로드
        loadCafesFromFirestore();
    }

    private void loadCafesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allCafes.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String name = document.getString("name");
                    String address = document.getString("address");
                    List<String> tagStrings = (List<String>) document.get("tags");
                    Tag[] tags = convertStringListToTags(tagStrings);

                    allCafes.add(new Recommender.CafeModel(
                            document.getId(), name, address, tags, 0.0, 0.0
                    ));
                }
                showRecommendations();
            } else {
                Toast.makeText(this, "데이터 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRecommendations() {
        List<Recommender.Recommendation> recommendations = Recommender.recommend(allCafes, selectedTags);
        adapter = new CafeAdapter(recommendations);
        recyclerView.setAdapter(adapter);
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            showRecommendations();
            return;
        }

        List<Recommender.Recommendation> filtered = new ArrayList<>();
        // 현재 어댑터에 있는 리스트에서 검색
        if (adapter != null && adapter.getResultList() != null) {
            for (Recommender.Recommendation rec : adapter.getResultList()) {
                if (rec.cafe.name.contains(query)) {
                    filtered.add(rec);
                }
            }
        }
        adapter.updateList(filtered);
    }

    private Tag[] convertStringListToTags(List<String> tagStrings) {
        List<Tag> validTags = new ArrayList<>();
        if (tagStrings != null) {
            for (String s : tagStrings) {
                try { validTags.add(Tag.valueOf(s)); }
                catch (Exception e) { }
            }
        }
        if (validTags.isEmpty()) validTags.add(Tag.DRINK_TASTY);
        return validTags.toArray(new Tag[0]);
    }
}