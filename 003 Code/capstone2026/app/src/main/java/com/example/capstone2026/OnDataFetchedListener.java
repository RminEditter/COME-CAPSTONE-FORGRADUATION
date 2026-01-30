package com.example.capstone2026;

public interface OnDataFetchedListener<T> {
    void onSuccess(T data);  // 성공했을 때 데이터를 넘겨줌
    void onFailure(Exception e); // 실패했을 때 에러를 넘겨줌
}