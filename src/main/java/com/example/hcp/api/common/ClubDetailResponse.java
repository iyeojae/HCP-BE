// src/main/java/com/example/hcp/api/common/ClubDetailResponse.java
package com.example.hcp.api.common;

import com.example.hcp.domain.club.entity.ClubCategory;

import java.time.LocalDateTime;
import java.util.List;

public record ClubDetailResponse(
        Long clubId,

        String mainImageUrl,
        String name,
        String summary,

        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,

        String recruitState,
        Long secondsToStart,
        Long secondsToEnd,

        long applicationCount,

        ClubCategory category,
        String introduction,
        String interviewProcess,

        long viewCount,

        // ✅ 대표사진(mainImageUrl) 제외한 club-level 추가 미디어(post=null)
        List<Media> media
) {
    public record Media(Long mediaId, String type, String url) {}
}
