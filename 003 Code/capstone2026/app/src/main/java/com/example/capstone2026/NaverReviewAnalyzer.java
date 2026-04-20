package com.example.capstone2026;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NaverReviewAnalyzer {
    private static final String CLIENT_ID = "개인키"; // 실제 키는 프로젝트 환경변수에서 관리하세요
    private static final String CLIENT_SECRET = "개인키";

    public interface AnalysisCallback {
        void onComplete(List<Tag> analyzedTags);
    }

    public static void analyzeCafe(String cafeName, String address, AnalysisCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // 검색어 조합: 예) "어은동 델리버리 카페 후기"
        String[] addrParts = address.split(" ");
        String dongName = (addrParts.length > 2) ? addrParts[2] : "";
        String query = dongName + " " + cafeName + " 후기";

        Request request = new Request.Builder()
                .url("https://openapi.naver.com/v1/search/blog.json?display=10&query=" + query)
                .addHeader("X-Naver-Client-Id", CLIENT_ID)
                .addHeader("X-Naver-Client-Secret", CLIENT_SECRET)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    List<Tag> tags = parseTagsFromRespones(jsonData);
                    callback.onComplete(tags);
                }
            }
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }
        });
    }

    private static List<Tag> parseTagsFromRespones(String jsonData) {
        List<Tag> resultTags = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray items = jsonObject.getJSONArray("items");
            StringBuilder allText = new StringBuilder();

            for (int i = 0; i < items.length(); i++) {
                allText.append(items.getJSONObject(i).getString("title"));
                allText.append(items.getJSONObject(i).getString("description"));
            }

            String content = allText.toString();

            /* ============================================================
               핵심 키워드 매칭 (새로운 Tag.java 시스템에 맞게 전면 수정)
               ============================================================ */

            // 1. 카공/작업 (WORK -> WORK_FRIENDLY)
            if (content.contains("카공") || content.contains("스터디") || content.contains("작업")) {
                resultTags.add(Tag.WORK_FRIENDLY);
            }

            // 2. 분위기/인테리어 (PHOTO -> INTERIOR_PRETTY)
            if (content.contains("사진") || content.contains("인스타") || content.contains("예쁜") || content.contains("인테리어")) {
                resultTags.add(Tag.INTERIOR_PRETTY);
            }

            // 3. 디저트 (유지)
            if (content.contains("디저트") || content.contains("케이크") || content.contains("빵") || content.contains("마카롱")) {
                resultTags.add(Tag.DESSERT);
            }

            // 4. 산미/스페셜티 (ACIDIC -> BEAN_ACIDIC, SPECIALTY_DRIP)
            if (content.contains("산미") || content.contains("신맛")) {
                resultTags.add(Tag.BEAN_ACIDIC);
            }
            if (content.contains("스페셜티") || content.contains("핸드드립") || content.contains("드립커피")) {
                resultTags.add(Tag.SPECIALTY_DRIP);
            }

            // 5. 고소한 맛 (NUTTY -> BEAN_NUTTY)
            if (content.contains("고소한") || content.contains("견과류") || content.contains("바디감")) {
                resultTags.add(Tag.BEAN_NUTTY);
            }

            // 6. 힙한 감성 (REST/QUIET 대체 -> HIP)
            if (content.contains("힙한") || content.contains("감성") || content.contains("요즘느낌")) {
                resultTags.add(Tag.HIP);
            }

            // 7. 음료 맛집 (추가 분석)
            if (content.contains("맛있다") || content.contains("존맛") || content.contains("최고")) {
                resultTags.add(Tag.DRINK_TASTY);
            }

        } catch (Exception e) { e.printStackTrace(); }
        return resultTags;
    }
}