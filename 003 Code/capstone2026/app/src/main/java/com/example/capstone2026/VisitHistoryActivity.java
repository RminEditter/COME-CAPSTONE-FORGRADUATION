package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
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

        setupBackButton();
        BottomNavHelper.setup(this);

        db = FirebaseFirestore.getInstance();

        rvVisitHistory = findViewById(R.id.rvVisitHistory);
        rvVisitHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVisitRecords();
    }

    private void setupBackButton() {

        AppCompatButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadVisitRecords() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "로그인이 필요합니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currentUid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        db.collection("visit_records")
                .whereEqualTo("userUid", currentUid)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() &&
                            task.getResult() != null) {

                        List<VisitRecord> records =
                                new ArrayList<>();

                        for (QueryDocumentSnapshot document :
                                task.getResult()) {

                            String cafeName =
                                    document.getString("cafeName");

                            Double ratingDouble =
                                    document.getDouble("rating");

                            float rating =
                                    ratingDouble != null
                                            ? ratingDouble.floatValue()
                                            : 0.0f;

                            String memo =
                                    document.getString("memo");

                            Long visitedAtLong =
                                    document.getLong("visitedAt");

                            long visitedAt =
                                    visitedAtLong != null
                                            ? visitedAtLong
                                            : 0L;

                            String uid =
                                    document.getString("userUid");

                            VisitRecord record =
                                    new VisitRecord(
                                            cafeName,
                                            rating,
                                            memo,
                                            visitedAt
                                    );

                            record.setId(document.getId());
                            record.setUserUid(uid);

                            records.add(record);
                        }

                        VisitHistoryAdapter adapter =
                                new VisitHistoryAdapter(records);

                        rvVisitHistory.setAdapter(adapter);

                    } else {

                        Toast.makeText(
                                this,
                                "서버에서 데이터를 가져오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}