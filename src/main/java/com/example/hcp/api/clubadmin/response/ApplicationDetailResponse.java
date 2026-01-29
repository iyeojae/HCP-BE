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
            Long questionId,
            Integer orderNo,
            Integer templateNo,
            String label,
            String payloadJson,
            String value
    ) {
        // 기존 호환용(원하면 제거 가능)
        public Answer(String label, String value) {
            this(null, null, null, label, null, value);
        }
    }
}
