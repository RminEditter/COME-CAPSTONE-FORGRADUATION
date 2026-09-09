package com.example.capstone2026;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NaverReviewAnalyzer {
    private static final String CLIENT_ID = ""; // 실제 네이버 Client ID 입력
    private static final String CLIENT_SECRET = ""; // 실제 네이버 Client Secret 입력

    public interface AnalysisCallback {
        void onComplete(List<Tag> analyzedTags);
    }

    public static void analyzeCafe(String cafeName, String address, AnalysisCallback callback) {
        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()
                || cafeName == null || cafeName.trim().isEmpty()) {
            callback.onComplete(new ArrayList<>());
            return;
        }
        OkHttpClient client = new OkHttpClient();

        String[] addrParts = address == null ? new String[0] : address.trim().split("\\s+");
        String dongName = (addrParts.length > 2) ? addrParts[2] : "";
        String query = dongName + " " + cafeName + " 후기";

        Request request = new Request.Builder()
                .url(new HttpUrl.Builder().scheme("https").host("openapi.naver.com")
                        .addPathSegments("v1/search/blog.json")
                        .addQueryParameter("display", "10")
                        .addQueryParameter("query", query).build())
                .addHeader("X-Naver-Client-Id", CLIENT_ID)
                .addHeader("X-Naver-Client-Secret", CLIENT_SECRET)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<Tag> tags = new ArrayList<>();
                try (Response closedResponse = response) {
                    if (closedResponse.isSuccessful() && closedResponse.body() != null) {
                        tags = parseTagsFromRespones(closedResponse.body().string());
                    } else {
                        android.util.Log.e("CafeFit_API", "API 응답 실패: " + closedResponse.code());
                    }
                } catch (IOException e) {
                    android.util.Log.e("CafeFit_API", "API 응답 읽기 실패", e);
                }
                callback.onComplete(tags);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("CafeFit_API", "네트워크 요청 실패: " + e.getMessage());
                callback.onComplete(new ArrayList<>());
            }
        });
    }

    private static List<Tag> parseTagsFromRespones(String jsonData) {
        List<Tag> resultTags = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray items = jsonObject.getJSONArray("items");
            StringBuilder allText = new StringBuilder();

            for (int i = 0; i < items.length(); i++) {
                allText.append(items.getJSONObject(i).getString("title")).append(" ");
                allText.append(items.getJSONObject(i).getString("description")).append(" ");
            }

            String content = allText.toString();

            /* ============================================================
               새로 세분화된 Tag enum 반영 키워드 매칭 로직
               ============================================================ */

            // 1. 원두 취향
            if (content.contains("고소한") || content.contains("견과류") || content.contains("바디감") ||
                    content.contains("묵직한") || content.contains("초콜릿")) {
                resultTags.add(Tag.BEAN_NUTTY);
            }
            if (content.contains("산미") || content.contains("신맛") || content.contains("과일향") ||
                    content.contains("화사한")) {
                resultTags.add(Tag.BEAN_ACIDIC);
            }

            // 2. 카페 성향
            if (content.contains("인테리어") || content.contains("예쁜") || content.contains("소품") ||
                    content.contains("인스타") || content.contains("사진 잘 나오는") || content.contains("뷰 맛집")) {
                resultTags.add(Tag.INTERIOR_PRETTY);
            }
            if (content.contains("커피 맛집") || content.contains("시그니처") || content.contains("원두 맛") ||
                    content.contains("커피가 맛있는") || content.contains("아메리카노 맛있는")) {
                resultTags.add(Tag.DRINK_TASTY);
            }
            if (content.contains("힙한") || content.contains("감성카페") || content.contains("요즘느낌") ||
                    content.contains("유니크") || content.contains("트렌디")) {
                resultTags.add(Tag.HIP);
            }
            if (content.contains("카공") || content.contains("스터디") || content.contains("작업하기 좋은") ||
                    content.contains("조용한") || content.contains("공부하기")) {
                resultTags.add(Tag.WORK_FRIENDLY);
            }

            // 3. 디저트
            if (content.contains("디저트") || content.contains("케이크") || content.contains("빵") ||
                    content.contains("구움과자") || content.contains("마카롱") || content.contains("스콘")) {
                resultTags.add(Tag.DESSERT);
            }

            // 4. 스페셜티
            if (content.contains("스페셜티") || content.contains("핸드드립") || content.contains("필터커피") ||
                    content.contains("원두 선택")) {
                resultTags.add(Tag.SPECIALTY_DRIP);
            }

            // 5. 카페 규모
            if (content.contains("대형 카페") || content.contains("넓은") || content.contains("규모가 큰") ||
                    content.contains("광활한") || content.contains("탁 트인")) {
                resultTags.add(Tag.LARGE_CAFE);
            } else if (content.contains("아담한") || content.contains("작은") || content.contains("소규모") ||
                    content.contains("동네 카페") || content.contains("아기자기한")) {
                resultTags.add(Tag.SMALL_CAFE);
            }

            // 6. 동반자
            if (content.contains("혼자") || content.contains("혼커") || content.contains("1인") || content.contains("혼밥")) {
                resultTags.add(Tag.SOLO);
            }
            if (content.contains("데이트") || content.contains("연인") || content.contains("커플") || content.contains("남자친구") || content.contains("여자친구")) {
                resultTags.add(Tag.COUPLE);
            }
            if (content.contains("친구") || content.contains("수다") || content.contains("모임") || content.contains("우정")) {
                resultTags.add(Tag.FRIEND);
            }
            if (content.contains("가족") || content.contains("부모님") || content.contains("아이들") || content.contains("애기") || content.contains("키즈존")) {
                resultTags.add(Tag.FAMILY);
            }
            if (content.contains("회식") || content.contains("점심시간") || content.contains("직장인") || content.contains("미팅") || content.contains("동료")) {
                resultTags.add(Tag.COLLEAGUE);
            }

            // 7. [신규 추가] 카공 세부 태그
            if (content.contains("콘센트") || content.contains("플러그") || content.contains("충전")) {
                resultTags.add(Tag.OUTLET_MANY);
            }
            if (content.contains("와이파이") || content.contains("wifi") || content.contains("인터넷")) {
                resultTags.add(Tag.WIFI_FAST);
            }
            if (content.contains("노트북") || content.contains("맥북") || content.contains("작업")) {
                resultTags.add(Tag.LAPTOP_OK);
            }

            return new ArrayList<>(new HashSet<>(resultTags));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultTags;
    }
}
