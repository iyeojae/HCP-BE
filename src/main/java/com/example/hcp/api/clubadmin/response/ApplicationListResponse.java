// src/main/java/com/example/hcp/api/clubadmin/response/ApplicationListResponse.java
package com.example.hcp.api.clubadmin.response;

import java.util.List;

public record ApplicationListResponse(List<Item> items) {
    public record Item(
            Long applicationId,
            Long userId,
            String studentNo,
            String name,
            String department,
            String status,
            String createdAt
    ) {}
}
