package com.example.capstone2026;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NaverReviewAnalyzer {
    private static final String CLIENT_ID = ""; // 실제 키는 프로젝트 환경변수에서 관리하세요
    private static final String CLIENT_SECRET = "";

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
                } else {
                    // API 키가 틀렸거나 한도 초과일 경우 여기서 찍힘
                    android.util.Log.e("CafeFit_API", "API 응답 실패: " + response.code() + " / " + response.message());
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 인터넷이 안 되거나 URL이 잘못된 경우 여기서 찍힘
                android.util.Log.e("CafeFit_API", "네트워크 요청 실패: " + e.getMessage());
                e.printStackTrace();
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
                allText.append(items.getJSONObject(i).getString("title"));
                allText.append(items.getJSONObject(i).getString("description"));
            }

            String content = allText.toString();

        /* ============================================================
           최신 태그 시스템 반영 키워드 매칭 로직
           ============================================================ */

            // 1. 작업/공부 환경 (WORK_FRIENDLY)
            if (content.contains("카공") || content.contains("스터디") || content.contains("작업하기 좋은") ||
                    content.contains("노트북") || content.contains("조용한") || content.contains("콘센트")) {
                resultTags.add(Tag.WORK_FRIENDLY);
            }

            // 2. 인테리어/사진 (INTERIOR_PRETTY)
            if (content.contains("인테리어") || content.contains("예쁜") || content.contains("소품") ||
                    content.contains("인스타") || content.contains("사진 잘 나오는") || content.contains("뷰 맛집")) {
                resultTags.add(Tag.INTERIOR_PRETTY);
            }

            // 3. 디저트 (DESSERT)
            if (content.contains("디저트") || content.contains("케이크") || content.contains("빵") ||
                    content.contains("구움과자") || content.contains("마카롱") || content.contains("스콘")) {
                resultTags.add(Tag.DESSERT);
            }

            // 4. 산미 있는 원두 (BEAN_ACIDIC)
            if (content.contains("산미") || content.contains("신맛") || content.contains("과일향") ||
                    content.contains("화사한")) {
                resultTags.add(Tag.BEAN_ACIDIC);
            }

            // 5. 고소한 원두 (BEAN_NUTTY)
            if (content.contains("고소한") || content.contains("견과류") || content.contains("바디감") ||
                    content.contains("묵직한") || content.contains("초콜릿")) {
                resultTags.add(Tag.BEAN_NUTTY);
            }

            // 6. 스페셜티/핸드드립 (SPECIALTY_DRIP)
            if (content.contains("스페셜티") || content.contains("핸드드립") || content.contains("필터커피") ||
                    content.contains("원두 선택")) {
                resultTags.add(Tag.SPECIALTY_DRIP);
            }

            // 7. 힙한 감성 (HIP)
            if (content.contains("힙한") || content.contains("감성카페") || content.contains("요즘느낌") ||
                    content.contains("유니크") || content.contains("트렌디")) {
                resultTags.add(Tag.HIP);
            }

            // 8. 음료가 맛있는 (DRINK_TASTY) - 커피 외 음료 포함
            if (content.contains("음료 맛집") || content.contains("시그니처") || content.contains("맛있다") ||
                    content.contains("존맛") || content.contains("추천메뉴")) {
                resultTags.add(Tag.DRINK_TASTY);
            }
            // 5. 카페 규모 (SMALL_CAFE / LARGE_CAFE)
            if (content.contains("대형 카페") || content.contains("넓은") || content.contains("규모가 큰") ||
                    content.contains("층") || content.contains("광활한") || content.contains("탁 트인")) {
                resultTags.add(Tag.LARGE_CAFE);
            } else if (content.contains("아담한") || content.contains("작은") || content.contains("소규모") ||
                    content.contains("동네 카페") || content.contains("아기자기한")) {
                resultTags.add(Tag.SMALL_CAFE);
            }

            // 6. 동반자 (SOLO, COUPLE, FRIEND, FAMILY, COLLEAGUE)
            // SOLO (혼자)
            if (content.contains("혼자") || content.contains("혼커") || content.contains("1인") || content.contains("혼밥")) {
                resultTags.add(Tag.SOLO);
            }
            // COUPLE (커플)
            if (content.contains("데이트") || content.contains("연인") || content.contains("커플") || content.contains("남자친구") || content.contains("여자친구")) {
                resultTags.add(Tag.COUPLE);
            }
            // FRIEND (친구)
            if (content.contains("친구") || content.contains("수다") || content.contains("모임") || content.contains("우정")) {
                resultTags.add(Tag.FRIEND);
            }
            // FAMILY (가족)
            if (content.contains("가족") || content.contains("부모님") || content.contains("아이들") || content.contains("애기") || content.contains("키즈존")) {
                resultTags.add(Tag.FAMILY);
            }
            // COLLEAGUE (동료/회식)
            if (content.contains("회식") || content.contains("점심시간") || content.contains("직장인") || content.contains("미팅") || content.contains("동료")) {
                resultTags.add(Tag.COLLEAGUE);
            }

            // 중복 제거 (여러 키워드가 걸릴 수 있으므로)
            return new ArrayList<>(new HashSet<>(resultTags));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultTags;
    }
}