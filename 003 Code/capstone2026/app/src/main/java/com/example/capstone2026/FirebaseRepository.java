package com.example.capstone2026;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FirebaseRepository {
    private FirebaseFirestore db;

    public FirebaseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // 1. 모든 카페 목록 가져오기
    public void getCafeList(OnDataFetchedListener<List<Cafe>> listener) {
        db.collection("cafes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Cafe> cafeList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    cafeList.add(document.toObject(Cafe.class));
                }
                listener.onSuccess(cafeList);
            } else {
                listener.onFailure(task.getException());
            }
        });
    }

    // 2. 이름으로 카페 검색하기
    public void searchCafeByName(String searchText, OnDataFetchedListener<List<Cafe>> listener) {
        db.collection("cafes")
                .orderBy("name")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Cafe> searchResults = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            searchResults.add(document.toObject(Cafe.class));
                        }
                        listener.onSuccess(searchResults);
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    /**
     * 네이버 API로 분석한 태그 리스트를 해당 카페 문서에 저장합니다.
     * @param cafeId 카페의 고유 ID (문서 ID)
     * @param tags 분석된 Tag 객체 리스트
     */
    public void updateCafeTags(String cafeId, List<Tag> tags) {
        if (cafeId == null || tags == null) return;

        // 1. Tag Enum 리스트를 String 리스트로 변환 (DB 저장용)
        List<String> tagStrings = new ArrayList<>();
        for (Tag tag : tags) {
            tagStrings.add(tag.name()); // 예: Tag.WORK -> "WORK"
        }

        // 2. Firestore의 해당 카페 문서 업데이트
        db.collection("cafes") // 태섭님의 컬렉션 이름 확인 (예: "cafes" 또는 "Cafe")
                .document(cafeId)
                .update("tags", tagStrings)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "태그 업데이트 성공: " + cafeId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "태그 업데이트 실패", e);
                });
    }
}