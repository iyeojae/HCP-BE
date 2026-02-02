// src/main/java/com/example/hcp/api/admin/AdminCreateClubRequest.java
package com.example.hcp.api.admin;

import com.example.hcp.domain.club.entity.ClubCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdminCreateClubRequest(
        @NotBlank String name,
        @NotBlank String summary,                 // 한줄소개
        @NotNull LocalDateTime recruitStartAt,    // 모집 시작
        @NotNull LocalDateTime recruitEndAt,      // 모집 마감
        @NotNull ClubCategory category,
        String introduction,                      // 소개 글(특수문자/이모지 허용)
        String interviewProcess                  // 모집절차 글(특수문자/이모지 허용)
) {}
