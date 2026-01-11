// src/main/java/com/example/hcp/api/clubadmin/request/ClubUpsertRequest.java
package com.example.hcp.api.clubadmin.request;

import com.example.hcp.domain.club.entity.ClubCategory;
import jakarta.validation.constraints.NotBlank;

public record ClubUpsertRequest(
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
