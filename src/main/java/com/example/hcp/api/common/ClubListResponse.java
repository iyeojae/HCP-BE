// src/main/java/com/example/hcp/api/common/ClubListResponse.java
package com.example.hcp.api.common;

import com.example.hcp.domain.club.entity.ClubCategory;

public record ClubListResponse(
        Long clubId,
        String name,
        ClubCategory category,
        String recruitmentStatus
) {}
