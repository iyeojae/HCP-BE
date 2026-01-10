// src/main/java/com/example/hcp/api/student/FormResponse.java
package com.example.hcp.api.student;

import java.util.List;

public record FormResponse(
        Long formId,
        Long clubId,
        List<String> labels
) {}
