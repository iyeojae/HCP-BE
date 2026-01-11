// src/main/java/com/example/hcp/api/common/ClubDetailResponse.java
package com.example.hcp.api.common;

import com.example.hcp.domain.club.entity.ClubCategory;

import java.util.List;

public record ClubDetailResponse(
        Long clubId,
        String name,
        String introduction,
        String activities,
        String recruitTarget,
        String interviewProcess,
        String contactLink,
        ClubCategory category,
        String recruitmentStatus,
        long viewCount,
        List<Media> media
) {
    public record Media(
            Long mediaId,
            String type,
            String url
    ) {}
}
