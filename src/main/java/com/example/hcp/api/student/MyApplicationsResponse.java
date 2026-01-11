// src/main/java/com/example/hcp/api/student/MyApplicationsResponse.java
package com.example.hcp.api.student;

public class MyApplicationsResponse {

    public record Item(
            Long applicationId,
            Long clubId,
            String clubName,
            String status,
            String createdAt
    ) {}
}
