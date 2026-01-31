// src/main/java/com/example/hcp/api/clubadmin/request/FormUpsertRequest.java
package com.example.hcp.api.clubadmin.request;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        // ✅ payload 래퍼 제거: Item 바로 아래에 템플릿별 필드가 위치
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Item(
                @NotNull Integer orderNo,

                @NotNull @Min(1) @Max(6)
                Integer templateNo,

                @NotBlank
                String title,

                // 템플릿별 사용 필드(서비스에서 templateNo 기준 검증)
                List<String> words,               // T1(11개), T6(8개)
                List<String> questions,           // T2(3개)
                List<String> sentences,           // T3(4개)

                @Valid
                TwoWordQuestions twoWordQuestions, // T4

                @Valid
                Template5Questions template5Questions // T5
        ) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record TwoWordQuestions(
                @NotBlank String question1Title,
                List<String> question1Words,
                @NotBlank String question2Title,
                List<String> question2Words
        ) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Template5Questions(
                @NotBlank String question1Title,
                List<Integer> numberOptions,     // 예: [0,1,2,3]
                List<Boolean> booleanOptions,    // 예: [true,false]
                @NotBlank String question2Title  // 자유서술 제목
        ) {}
}
