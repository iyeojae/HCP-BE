// src/main/java/com/example/hcp/api/admin/AdminClubController.java
package com.example.hcp.api.admin;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.service.ClubCommandService;
import com.example.hcp.domain.content.service.ContentCommandService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClubController {

    private final ClubCommandService clubCommandService;
    private final ContentCommandService contentCommandService;
    private final ObjectMapper objectMapper;

    public AdminClubController(
            ClubCommandService clubCommandService,
            ContentCommandService contentCommandService,
            ObjectMapper objectMapper
    ) {
        this.clubCommandService = clubCommandService;
        this.contentCommandService = contentCommandService;
        this.objectMapper = objectMapper;
    }

    // ✅ 관리자: 동아리 생성(동아리 필드 + 메인사진(필수) + 사진/영상 여러개(옵션))
    @PostMapping(value = "/clubs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCreateClubResponse createClub(
            @RequestPart("data") String dataJson,
            @RequestPart("mainImage") MultipartFile mainImage,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles
    ) {
        AdminCreateClubRequest req = parseAdminCreateClubRequest(dataJson);

        requireText(req.name(), "NAME_REQUIRED");
        requireText(req.summary(), "SUMMARY_REQUIRED");
        requireNotNull(req.recruitStartAt(), "RECRUIT_START_REQUIRED");
        requireNotNull(req.recruitEndAt(), "RECRUIT_END_REQUIRED");
        requireNotNull(req.category(), "CATEGORY_REQUIRED");

        validateRecruitPeriod(req.recruitStartAt(), req.recruitEndAt());
        requireNonEmpty(mainImage, "MAIN_IMAGE_REQUIRED");
        requireImage(mainImage, "MAIN_IMAGE_MUST_BE_IMAGE");

        Club c = new Club();
        c.setName(req.name());
        c.setSummary(req.summary());
        c.setRecruitStartAt(req.recruitStartAt());
        c.setRecruitEndAt(req.recruitEndAt());
        c.setCategory(req.category());
        c.setIntroduction(req.introduction());
        c.setInterviewProcess(req.interviewProcess());

        Club saved = clubCommandService.create(c);

        // 메인사진(대표사진) - cover는 "첫 IMAGE" 규칙이라 반드시 첫 업로드
        contentCommandService.uploadMedia(saved.getId(), null, mainImage);

        // ✅ 관련 사진/영상 여러개(옵션): image/* 또는 video/*만 허용
        if (mediaFiles != null) {
            for (MultipartFile f : mediaFiles) {
                if (f != null && !f.isEmpty()) {
                    requireImageOrVideo(f, "MEDIA_FILE_MUST_BE_IMAGE_OR_VIDEO");
                    contentCommandService.uploadMedia(saved.getId(), null, f);
                }
            }
        }

        return new AdminCreateClubResponse(saved.getId());
    }

    private AdminCreateClubRequest parseAdminCreateClubRequest(String dataJson) {
        if (!StringUtils.hasText(dataJson)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "DATA_REQUIRED");
        }
        try {
            return objectMapper.readValue(dataJson, AdminCreateClubRequest.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_DATA_JSON");
        }
    }

    private void validateRecruitPeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return;
        if (!end.isAfter(start)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "RECRUIT_END_MUST_BE_AFTER_START");
        }
    }

    private void requireNonEmpty(MultipartFile file, String code) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
    }

    private void requireImage(MultipartFile file, String code) {
        String ct = file.getContentType();
        if (!StringUtils.hasText(ct) || !ct.toLowerCase().startsWith("image/")) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
    }

    // ✅ 추가: image/* 또는 video/*만 허용
    private void requireImageOrVideo(MultipartFile file, String code) {
        String ct = file.getContentType();
        if (!StringUtils.hasText(ct)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
        String lower = ct.toLowerCase();
        if (!lower.startsWith("image/") && !lower.startsWith("video/")) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
    }

    private void requireText(String s, String code) {
        if (!StringUtils.hasText(s)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
    }

    private void requireNotNull(Object o, String code) {
        if (o == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
    }
}
