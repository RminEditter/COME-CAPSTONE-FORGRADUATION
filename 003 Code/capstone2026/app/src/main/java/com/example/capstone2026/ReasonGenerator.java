package com.example.capstone2026;

public class ReasonGenerator {

    public static String buildReason(
            Tag[] matchedTags,
            double distanceMeters,
            Tag priorityTag
    ) {
        StringBuilder reason = new StringBuilder();

        reason.append("추천 이유\n");

        // 1. 일치한 태그 표시
        if (matchedTags != null && matchedTags.length > 0) {

            int limit = Math.min(3, matchedTags.length);

            for (int i = 0; i < limit; i++) {
                Tag tag = matchedTags[i];

                if (tag == null) {
                    continue;
                }

                reason.append("✓ ")
                        .append(tag.getKoreanLabel())
                        .append(" 취향과 일치해요");

                // 중요 조건에 해당하는 태그 표시
                if (isPriorityMatched(tag, priorityTag)) {
                    reason.append(" · 중요 조건");
                }

                reason.append("\n");
            }
        } else {
            reason.append("✓ 현재 위치와 카페 정보를 바탕으로 추천했어요\n");
        }

        // 2. 거리 정보 표시
        reason.append("✓ 현재 위치에서 ");

        if (distanceMeters < 1000) {
            reason.append("약 ")
                    .append((int) distanceMeters)
                    .append("m 떨어져 있어요");
        } else {
            reason.append("약 ")
                    .append(String.format("%.1f", distanceMeters / 1000.0))
                    .append("km 떨어져 있어요");
        }

        // 3. 거리 우선 조건 표시
        if (priorityTag == Tag.DISTANCE) {
            reason.append("\n✓ 가까운 거리를 중요하게 반영했어요");
        }

        return reason.toString();
    }

    private static boolean isPriorityMatched(Tag matchedTag, Tag priorityTag) {

        if (matchedTag == null || priorityTag == null) {
            return false;
        }

        // 직접 선택한 중요 태그
        if (matchedTag == priorityTag) {
            return true;
        }

        // 분위기 우선 선택 시 분위기 관련 태그
        if (priorityTag == Tag.MOOD) {
            return matchedTag == Tag.INTERIOR_PRETTY
                    || matchedTag == Tag.HIP
                    || matchedTag == Tag.WORK_FRIENDLY
                    || matchedTag == Tag.SMALL_CAFE
                    || matchedTag == Tag.LARGE_CAFE;
        }

        // 작업하기 편함 우선 선택 시 카공 관련 태그
        if (priorityTag == Tag.WORK_FRIENDLY) {
            return matchedTag == Tag.WORK_FRIENDLY
                    || matchedTag == Tag.OUTLET_MANY
                    || matchedTag == Tag.WIFI_FAST
                    || matchedTag == Tag.LAPTOP_OK;
        }

        return false;
    }
}