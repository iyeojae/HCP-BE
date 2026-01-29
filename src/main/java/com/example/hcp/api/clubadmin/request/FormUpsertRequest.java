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

        @NotNull
        Integer totalItems,

        @NotEmpty
        @Valid
        List<Item> items
) {
        public record Item(
                @NotNull
                Integer orderNo,

                @NotNull
                @Min(1) @Max(6)
                Integer templateNo,

                @NotBlank
                String title,

                @NotNull
                @Valid
                Payload payload
        ) {}

        public record Payload(

                // [템플릿 1] 11개 / [템플릿 6] 8개
                List<String> words,

                // [템플릿 2] 질문 3개
                List<String> questions,

                // [템플릿 3] 문장 4개
                List<String> sentences,

                // [템플릿 4]
                TwoWordQuestions twoWordQuestions,

                // [템플릿 5] ✅ 1번문항: 숫자옵션 + boolean옵션 / 2번문항: 자유서술(제목만)
                Template5Questions template5Questions
        ) {}

        public record TwoWordQuestions(
                @NotBlank String question1Title,
                List<String> question1Words,
                @NotBlank String question2Title,
                List<String> question2Words
        ) {}

        public record Template5Questions(
                @NotBlank String question1Title,
                List<Integer> numberOptions,     // 예: [0,1,2,3]
                List<Boolean> booleanOptions,    // 예: [true,false]
                @NotBlank String question2Title  // 자유서술 제목
        ) {}
}
