// src/main/java/com/example/hcp/api/clubadmin/response/ApplicationListResponse.java
package com.example.hcp.api.clubadmin.response;

public record ApplicationListResponse(
        Long applicationId,
        Long userId,
        String studentNo,
        String name,
        String department,
        String status,
        String createdAt
) {}
