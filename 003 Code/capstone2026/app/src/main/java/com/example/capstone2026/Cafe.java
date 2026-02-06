package com.example.capstone2026;

import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Cafe {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;

    // Firebase의 "tags" 필드와 이름을 맞춥니다.
    @PropertyName("tags")
    private List<String> tags

    public Cafe() { } // Firebase용 필수 생성자

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @PropertyName("tags")
    public List<String> getTags() {
        return tags == null ? new ArrayList<>() : tags;
    }

    @PropertyName("tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}