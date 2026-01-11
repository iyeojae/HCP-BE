// src/main/java/com/example/hcp/api/admin/AdminCreateClubRequest.java
package com.example.hcp.api.admin;

import com.example.hcp.domain.club.entity.ClubCategory;
import jakarta.validation.constraints.NotBlank;

public record AdminCreateClubRequest(
        @NotBlank String name,
        String introduction,
        String activities,
        String recruitTarget,
        String interviewProcess,
        String contactLink,
        ClubCategory category,
        String recruitmentStatus,
        boolean isPublic
) {}
