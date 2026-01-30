package com.example.capstone2026;


import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NaverReviewAnalyzer {
    private static final String CLIENT_ID = "YOUR_NAVER_CLIENT_ID"; // 실제 키 삭제 완료
    private static final String CLIENT_SECRET = "YOUR_NAVER_CLIENT_SECRET";

    public interface AnalysisCallback {
        void onComplete(List<Tag> analyzedTags);
    }

    public static void analyzeCafe(String cafeName, String address, AnalysisCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // 검색어 조합: "어은동 델리버리 카페 후기"
        String query = address.split(" ")[2] + " " + cafeName + " 후기";

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

            // 핵심 키워드 매칭 (리뷰 기반)
            if (content.contains("카공") || content.contains("스터디") || content.contains("작업")) resultTags.add(Tag.WORK);
            if (content.contains("조용한") || content.contains("한적한")) resultTags.add(Tag.QUIET);
            if (content.contains("디저트") || content.contains("케이크") || content.contains("빵")) resultTags.add(Tag.DESSERT);
            if (content.contains("산미") || content.contains("커피가 신")) resultTags.add(Tag.ACIDIC);
            if (content.contains("고소한") || content.contains("너티")) resultTags.add(Tag.NUTTY);
            if (content.contains("사진") || content.contains("인스타") || content.contains("예쁜")) resultTags.add(Tag.PHOTO);
            if (content.contains("콘센트") || content.contains("충전")) resultTags.add(Tag.OUTLET);

        } catch (Exception e) { e.printStackTrace(); }
        return resultTags;
    }
}