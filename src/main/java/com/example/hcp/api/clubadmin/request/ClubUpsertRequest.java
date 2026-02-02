// src/main/java/com/example/hcp/api/clubadmin/request/ClubUpsertRequest.java
package com.example.hcp.api.clubadmin.request;

import com.example.hcp.domain.club.entity.ClubCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ClubUpsertRequest(
        @NotBlank String name,

        @NotBlank String summary,                 // 동아리 한줄소개
        @NotNull LocalDateTime recruitStartAt,    // 모집 시작날짜/시간
        @NotNull LocalDateTime recruitEndAt,      // 모집 마감날짜/시간

        @NotNull ClubCategory category,

        String introduction,                      // 소개 글(특수문자/이모지 포함 가능)
        String interviewProcess                  // 모집절차 글(특수문자/이모지 포함 가능
) {}
