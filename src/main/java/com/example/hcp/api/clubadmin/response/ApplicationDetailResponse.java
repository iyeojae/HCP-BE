// src/main/java/com/example/hcp/api/clubadmin/response/ApplicationDetailResponse.java
package com.example.hcp.api.clubadmin.response;

import java.util.List;

public record ApplicationDetailResponse(
        Long applicationId,
        Long userId,
        String studentNo,
        String name,
        String department,
        String status,
        String createdAt,
        List<Answer> answers
) {
    public record Answer(
            String label,
            String value
    ) {}
}
