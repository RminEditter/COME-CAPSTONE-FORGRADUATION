package com.example.graduation;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CafeCsvLoader {

    /**
     * cafe_01.csv 컬럼(예상):
     * 상가업소번호, 상호명, 상권업종소분류명, 시군구명, 행정동명, 도로명주소, 경도, 위도
     */
    public static List<Recommender.CafeModel> loadFromAssets(Context context, String assetFileName) {
        List<Recommender.CafeModel> cafes = new ArrayList<>();

        BufferedReader br = null;
        try {
            InputStream is = context.getAssets().open(assetFileName);
            br = new BufferedReader(new InputStreamReader(is, "EUC-KR"));

            String header = br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
                // 단순 split (주소에 쉼표가 들어가면 깨질 수 있음)
                String[] c = line.split(",", -1);
                if (c.length < 8) continue;

                String storeNo = c[0].trim();
                String name = c[1].trim();
                String category = c[2].trim();
                String district = c[3].trim();
                String dong = c[4].trim();
                String address = c[5].trim();

                double lng = safeDouble(c[6]);
                double lat = safeDouble(c[7]);

                if (lat == 0.0 || lng == 0.0) continue; // 좌표 없으면 스킵

                Tag[] tags = TagAssigner.assignTags(storeNo, name, address);

                long id = stableId(storeNo);

                // MVP: priceLevel/ratingAvg는 간단 처리
                int priceLevel = 3;
                float ratingAvg = 0f;

                cafes.add(new Recommender.CafeModel(id, name, lat, lng, tags, priceLevel, ratingAvg));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignored) {}
        }

        return cafes;
    }

    private static double safeDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    private static long stableId(String storeNo) {
        // 문자열 -> long 고정 변환
        return (long) storeNo.hashCode() & 0xffffffffL;
    }
}
