// src/main/java/com/example/hcp/api/admin/AdminCreateClubRequest.java
package com.example.hcp.api.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminCreateClubRequest(
        @NotBlank String name,
        String introduction,
        String activities,
        String recruitTarget,
        String interviewProcess,
        String contactLink,
        String category,
        String recruitmentStatus,
        boolean isPublic
) {}
