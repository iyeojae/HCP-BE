// src/main/java/com/example/hcp/api/student/SubmitApplicationRequest.java
package com.example.hcp.api.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitApplicationRequest(
        @NotEmpty
        List<@NotBlank String> answers
) {}
