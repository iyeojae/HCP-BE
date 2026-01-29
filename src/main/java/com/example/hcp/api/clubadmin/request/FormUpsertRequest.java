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

        // ✅ null 필드(words/questions/...)는 JSON에서 아예 빠짐
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Payload(
                List<String> words,
                List<String> questions,
                List<String> sentences,
                TwoWordQuestions twoWordQuestions,
                Template5Questions template5Questions
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
