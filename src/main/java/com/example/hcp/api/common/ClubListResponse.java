// src/main/java/com/example/hcp/api/common/ClubListResponse.java
package com.example.hcp.api.common;

public record ClubListResponse(
        Long clubId,
        String name,
        String imageUrl,   // 대표사진(없으면 null)
        String summary     // 한줄소개(공백 정리)
) {}
