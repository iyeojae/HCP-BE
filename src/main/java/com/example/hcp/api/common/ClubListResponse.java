package com.example.hcp.api.common;

public record ClubListResponse(
        Long clubId,
        String name,
        String category,
        String recruitmentStatus
) {}
