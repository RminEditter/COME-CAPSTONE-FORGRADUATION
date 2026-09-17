package com.example.capstone2026;

import java.util.List;

final class CafeMapItem {
        String id, name, address, tags;
        List<Tag> rawTags;
        double latitude, longitude;

        CafeMapItem(String id, String name, String address, String tags, List<Tag> rawTags, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.tags = tags;
            this.rawTags = rawTags;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
