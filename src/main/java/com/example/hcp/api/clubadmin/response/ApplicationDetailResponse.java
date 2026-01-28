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
    /**
     * ✅ 확장: 질문(템플릿) 메타 + 답변을 같이 내려줄 수 있게 필드 추가
     * - 기존 코드( label, value )로 생성하던 부분이 깨지지 않도록 보조 생성자 제공
     */
    public record Answer(
            Long questionId,
            Integer orderNo,
            Integer templateNo,
            String label,
            String payloadJson,
            String value
    ) {
        // ✅ 기존 호환용 생성자 (기존 컨트롤러 코드 유지 가능)
        public Answer(String label, String value) {
            this(null, null, null, label, null, value);
        }
    }
}
