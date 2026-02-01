// src/main/java/com/example/hcp/api/student/SubmitApplicationRequest.java
package com.example.hcp.api.student;

import java.util.List;

public record SubmitApplicationRequest(
        List<Object> answers
) {}
