package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VisitHistoryActivity extends AppCompatActivity {

    private RecyclerView rvVisitHistory;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_history);
        BottomNavHelper.setup(this);

        db = FirebaseFirestore.getInstance();

        androidx.appcompat.widget.AppCompatButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvVisitHistory = findViewById(R.id.rvVisitHistory);
        rvVisitHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVisitRecords();
    }

    private void loadVisitRecords() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // [수정] 복합 색인 에러 방지를 위해 orderBy를 완전히 제거하고 내 글만 솎아옵니다.
        db.collection("visit_records")
                .whereEqualTo("userUid", currentUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<VisitRecord> records = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String cafeName = document.getString("cafeName");
                            Double ratingDouble = document.getDouble("rating");
                            float rating = ratingDouble != null ? ratingDouble.floatValue() : 0.0f;
                            String memo = document.getString("memo");
                            Long visitedAtLong = document.getLong("visitedAt");
                            long visitedAt = visitedAtLong != null ? visitedAtLong : 0L;
                            String uid = document.getString("userUid");

                            VisitRecord record = new VisitRecord(cafeName, rating, memo, visitedAt);
                            record.setId(document.getId());
                            record.setUserUid(uid);

                            records.add(record);
                        }

                        VisitHistoryAdapter adapter = new VisitHistoryAdapter(records);
                        rvVisitHistory.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "서버에서 데이터를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}