package com.example.hcp.api.student;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitApplicationRequest(
        @NotEmpty List<Answer> answers
) {
    public record Answer(
            Long questionId,
            String value
    ) {}
}
