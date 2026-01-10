package com.example.hcp.api.student;

import java.util.List;

public record FormResponse(
        Long formId,
        Long clubId,
        List<Question> questions
) {
    public record Question(
            Long questionId,
            int orderNo,
            String label,
            String type,
            boolean required,
            String optionsJson
    ) {}
}
