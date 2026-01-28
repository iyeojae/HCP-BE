// src/main/java/com/example/hcp/api/clubadmin/request/FormUpsertRequest.java
package com.example.hcp.api.clubadmin.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FormUpsertRequest(

        // "총 몇개 질문(블록)을 만들었는지"
        @NotNull
        Integer totalItems,

        // orderNo 순서대로 화면에 노출
        @NotEmpty
        @Valid
        List<Item> items
) {
        public record Item(
                @NotNull
                Integer orderNo,

                // 1~6 중 하나
                @NotNull
                @Min(1) @Max(6)
                Integer templateNo,

                // 질문 제목(블록 제목)
                @NotBlank
                String title,

                // 템플릿별 구성값(선택 필드들)
                @Valid
                Payload payload
        ) {}

        public record Payload(

                // [템플릿 1] 단어 11개 / [템플릿 6] 단어 8개 (서비스에서 templateNo 기준으로 검증)
                List<String> words,

                // [템플릿 2] 질문 3개 (각 질문은 0~3 선택으로 답변)
                List<String> questions,

                // [템플릿 3] 문장 4개 중 선택
                List<String> sentences,

                // [템플릿 4] 질문 2개 + 각 질문별 단어 리스트
                TwoWordQuestions twoWordQuestions,

                // [템플릿 5] 질문 2개(자유서술 등은 서비스/프론트에서 처리)
                TwoTextQuestions twoTextQuestions
        ) {}

        public record TwoWordQuestions(
                @NotBlank String question1Title,
                List<String> question1Words,
                @NotBlank String question2Title,
                List<String> question2Words
        ) {}

        public record TwoTextQuestions(
                @NotBlank String question1Title,
                @NotBlank String question2Title
        ) {}
}
