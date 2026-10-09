package com.example.capstone2026;

public class TagVerification {

    public enum Status {
        VERIFIED,
        SPLIT,
        LOW_CONFIDENCE,
        COLLECTING
    }

    private final Tag tag;
    private final int selectedCount;
    private final int totalEvaluationCount;
    private final double ratio;
    private final boolean originalTag;
    private final Status status;

    public TagVerification(
            Tag tag,
            int selectedCount,
            int totalEvaluationCount,
            boolean originalTag
    ) {

        this.tag = tag;
        this.selectedCount = selectedCount;
        this.totalEvaluationCount = totalEvaluationCount;
        this.originalTag = originalTag;

        if (totalEvaluationCount > 0) {
            this.ratio =
                    (double) selectedCount
                            / totalEvaluationCount;
        } else {
            this.ratio = 0.0;
        }

        // 평가자 3명 미만
        if (totalEvaluationCount < 3) {

            this.status = Status.COLLECTING;

            // 70% 이상
        } else if (ratio >= 0.70) {

            this.status = Status.VERIFIED;

            // 40 ~ 69%
        } else if (ratio >= 0.40) {

            this.status = Status.SPLIT;

            // 40% 미만
        } else {

            this.status = Status.LOW_CONFIDENCE;
        }
    }


    public Tag getTag() {
        return tag;
    }


    public int getSelectedCount() {
        return selectedCount;
    }


    public int getTotalEvaluationCount() {
        return totalEvaluationCount;
    }


    public double getRatio() {
        return ratio;
    }


    public int getPercentage() {

        return (int) Math.round(
                ratio * 100.0
        );
    }


    public boolean isOriginalTag() {
        return originalTag;
    }


    public Status getStatus() {
        return status;
    }


    public boolean isVerified() {

        return status
                == Status.VERIFIED;
    }


    public boolean isDiscoveredTag() {

        return !originalTag
                && status
                == Status.VERIFIED;
    }


    public String getStatusText() {

        switch (status) {

            case VERIFIED:

                if (originalTag) {
                    return "✓ 사용자 검증";
                } else {
                    return "+ 사용자 발견";
                }


            case SPLIT:

                return "△ 의견 나뉨";


            case LOW_CONFIDENCE:

                return "신뢰도 낮음";


            case COLLECTING:

            default:

                return "데이터 수집 중";
        }
    }
}