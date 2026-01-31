// src/main/java/com/example/hcp/api/student/SubmitApplicationRequest.java
package com.example.hcp.api.student;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitApplicationRequest(
        @NotEmpty
        @Size(max = 50)
        List<@NotNull JsonNode> answers
) {}
