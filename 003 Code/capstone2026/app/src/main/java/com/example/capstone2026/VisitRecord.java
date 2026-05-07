package com.example.capstone2026;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "visit_records")
public class VisitRecord {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String cafeName;
    private float rating;
    private String memo;
    private long visitedAt;

    public VisitRecord(String cafeName, float rating, String memo, long visitedAt) {
        this.cafeName = cafeName;
        this.rating = rating;
        this.memo = memo;
        this.visitedAt = visitedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getCafeName() {
        return cafeName;
    }

    public float getRating() {
        return rating;
    }

    public String getMemo() {
        return memo;
    }

    public long getVisitedAt() {
        return visitedAt;
    }

    public void setCafeName(String cafeName) {
        this.cafeName = cafeName;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setVisitedAt(long visitedAt) {
        this.visitedAt = visitedAt;
    }
}