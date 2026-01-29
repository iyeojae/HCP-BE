// src/main/java/com/example/hcp/api/student/FormResponse.java
package com.example.hcp.api.student;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record FormResponse(
        Integer totalItems,
        List<Item> items
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            Integer orderNo,
            Integer templateNo,
            String title,

            List<String> words,
            List<String> questions,
            List<String> sentences,
            TwoWordQuestions twoWordQuestions,
            Template5Questions template5Questions
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
