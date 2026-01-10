package com.example.hcp.api.clubadmin.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FormUpsertRequest(
        @NotEmpty List<Question> questions
) {
    public record Question(
            int orderNo,
            String label,
            String type,
            boolean required,
            String optionsJson
    ) {}
}
