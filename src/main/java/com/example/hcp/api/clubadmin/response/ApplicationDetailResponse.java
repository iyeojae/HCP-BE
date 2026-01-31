// src/main/java/com/example/hcp/api/clubadmin/response/ApplicationDetailResponse.java
package com.example.hcp.api.clubadmin.response;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Answer(
            Integer orderNo,
            Integer templateNo,
            String title,

            // 템플릿별 필드(학생 폼 확인 응답과 동일)
            List<String> words,                // T1/T6
            List<String> questions,            // T2
            List<String> sentences,            // T3
            TwoWordQuestions twoWordQuestions, // T4
            Template5Questions template5Questions, // T5

            // ✅ 지원자 답변(저장된 valueText를 JSON이면 Object로 파싱해서 그대로)
            Object value
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TwoWordQuestions(
            String question1Title,
            List<String> question1Words,
            String question2Title,
            List<String> question2Words
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Template5Questions(
            String question1Title,
            List<Integer> numberOptions,
            List<Boolean> booleanOptions,
            String question2Title
    ) {}
}
