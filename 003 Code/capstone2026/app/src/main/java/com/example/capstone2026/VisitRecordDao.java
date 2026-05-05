package com.example.capstone2026;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VisitRecordDao {
    @androidx.room.Delete
    void delete(VisitRecord record);
    @Insert
    void insert(VisitRecord record);

    @Query("SELECT * FROM visit_records ORDER BY visitedAt DESC")
    List<VisitRecord> getAllRecords();

    @Query("SELECT * FROM visit_records WHERE cafeName = :cafeName ORDER BY visitedAt DESC")
    List<VisitRecord> getRecordsByCafe(String cafeName);
}