package com.example.capstone2026;

public class VisitRecord {

    private String id; // Firestore 문서 고유 ID
    private String cafeName;
    private float rating;
    private String memo;
    private long visitedAt;
    private String userUid; // 유저 고유 UID 필드 추가

    public VisitRecord() {
        // 파이어베이스 필수 빈 생성자
    }

    public VisitRecord(String cafeName, float rating, String memo, long visitedAt) {
        this.cafeName = cafeName;
        this.rating = rating;
        this.memo = memo;
        this.visitedAt = visitedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCafeName() { return cafeName; }
    public void setCafeName(String cafeName) { this.cafeName = cafeName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public long getVisitedAt() { return visitedAt; }
    public void setVisitedAt(long visitedAt) { this.visitedAt = visitedAt; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    // 어댑터 호환용 메소드
    public String getFbId() { return id; }
    public void setFbId(String fbId) { this.id = id; }
}