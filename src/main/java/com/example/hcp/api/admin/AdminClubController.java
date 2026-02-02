// src/main/java/com/example/hcp/api/admin/AdminClubController.java
package com.example.hcp.api.admin;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.service.ClubCommandService;
import com.example.hcp.domain.content.service.ContentCommandService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClubController {

    private final ClubCommandService clubCommandService;
    private final ContentCommandService contentCommandService;

    public AdminClubController(
            ClubCommandService clubCommandService,
            ContentCommandService contentCommandService
    ) {
        this.clubCommandService = clubCommandService;
        this.contentCommandService = contentCommandService;
    }

    // ✅ 관리자: 동아리 생성(동아리 필드 + 메인사진(필수) + 사진/영상 여러개(옵션))
    @PostMapping(value = "/clubs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCreateClubResponse createClub(
            @Valid @RequestPart("data") AdminCreateClubRequest req,
            @RequestPart("mainImage") MultipartFile mainImage,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles
    ) {
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

        // 관련 사진/영상 여러개(옵션)
        if (mediaFiles != null) {
            for (MultipartFile f : mediaFiles) {
                if (f != null && !f.isEmpty()) {
                    contentCommandService.uploadMedia(saved.getId(), null, f);
                }
            }
        }

        return new AdminCreateClubResponse(saved.getId());
    }

    private void validateRecruitPeriod(java.time.LocalDateTime start, java.time.LocalDateTime end) {
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
}
