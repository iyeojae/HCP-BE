package com.example.hcp.api.common;

import java.util.List;

public record ClubDetailResponse(
        Long clubId,
        String name,
        String introduction,
        String activities,
        String recruitTarget,
        String interviewProcess,
        String contactLink,
        String category,
        String recruitmentStatus,
        long viewCount,
        List<Post> posts
) {
    public record Post(
            Long postId,
            String title,
            String content,
            List<Media> media
    ) {}

    public record Media(
            Long mediaId,
            String type,
            String url
    ) {}
}
