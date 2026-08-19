package com.example.capstone2026;

public class CommunityPost {

    private String id;
    private String userUid;
    private String nickname;
    private String content;
    private long createdAt;

    public CommunityPost() {
    }

    public CommunityPost(
            String userUid,
            String nickname,
            String content,
            long createdAt
    ) {
        this.userUid = userUid;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}