// src/main/java/com/example/hcp/api/clubadmin/ClubAdminController.java
package com.example.hcp.api.clubadmin;

import com.example.hcp.api.clubadmin.request.ChangeStatusRequest;
import com.example.hcp.api.clubadmin.request.ClubUpsertRequest;
import com.example.hcp.api.clubadmin.request.FormUpsertRequest;
import com.example.hcp.api.clubadmin.response.*;
import com.example.hcp.domain.account.service.ClubAccessService;
import com.example.hcp.domain.application.entity.Application;
import com.example.hcp.domain.application.entity.ApplicationAnswer;
import com.example.hcp.domain.application.service.ApplicationAdminService;
import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.service.ClubCommandService;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.repository.MediaFileRepository;
import com.example.hcp.domain.content.service.ContentCommandService;
import com.example.hcp.domain.form.entity.FormQuestion;
import com.example.hcp.domain.form.service.FormCommandService;
import com.example.hcp.domain.stats.service.ClubDashboardService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.example.hcp.global.security.SecurityUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubadmin")
@PreAuthorize("hasRole('CLUB_ADMIN') or hasRole('ADMIN')")
public class ClubAdminController {

    private final ClubAccessService clubAccessService;
    private final ClubCommandService clubCommandService;
    private final ContentCommandService contentCommandService;
    private final MediaFileRepository mediaFileRepository;
    private final FormCommandService formCommandService;
    private final ApplicationAdminService applicationAdminService;
    private final ClubDashboardService clubDashboardService;
    private final ObjectMapper objectMapper;

    public ClubAdminController(
            ClubAccessService clubAccessService,
            ClubCommandService clubCommandService,
            ContentCommandService contentCommandService,
            MediaFileRepository mediaFileRepository,
            FormCommandService formCommandService,
            ApplicationAdminService applicationAdminService,
            ClubDashboardService clubDashboardService,
            ObjectMapper objectMapper
    ) {
        this.clubAccessService = clubAccessService;
        this.clubCommandService = clubCommandService;
        this.contentCommandService = contentCommandService;
        this.mediaFileRepository = mediaFileRepository;
        this.formCommandService = formCommandService;
        this.applicationAdminService = applicationAdminService;
        this.clubDashboardService = clubDashboardService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/clubs")
    public MyClubsResponse myClubs(@AuthenticationPrincipal SecurityUser me) {
        return new MyClubsResponse(clubAccessService.myClubIds(me.userId()));
    }

    // ✅ 동아리 수정(동아리 필드 + 메인사진(옵션) + 사진/영상 여러개(옵션))
    // - mainImage가 오면: 기존 club-level 미디어(post=null) 전체 교체 (새 main = isMain=true)
    // - mainImage 없이 mediaFiles만 오면: 기존 main(isMain=true) 유지, 나머지 club-level 미디어만 교체
    @PutMapping(value = "/clubs/{clubId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void updateClub(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @Valid @RequestPart("data") ClubUpsertRequest req,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles
    ) {
        assertClubAdminOrAdmin(me, clubId);
        validateRecruitPeriod(req.recruitStartAt(), req.recruitEndAt());

        Club changes = new Club();
        changes.setName(req.name());
        changes.setSummary(req.summary());
        changes.setRecruitStartAt(req.recruitStartAt());
        changes.setRecruitEndAt(req.recruitEndAt());
        changes.setCategory(req.category());
        changes.setIntroduction(req.introduction());
        changes.setInterviewProcess(req.interviewProcess());

        clubCommandService.update(clubId, changes);

        boolean hasMain = (mainImage != null && !mainImage.isEmpty());
        boolean hasMedia = hasAnyFile(mediaFiles);
        if (!hasMain && !hasMedia) return;

        if (hasMain) {
            requireImage(mainImage, "MAIN_IMAGE_MUST_BE_IMAGE");

            // 전체 교체
            List<MediaFile> oldAll = mediaFileRepository.findByClub_IdAndPostIsNullOrderByIdAsc(clubId);
            if (!oldAll.isEmpty()) mediaFileRepository.deleteAll(oldAll);

            // uploadMedia 내부에서: club-level IMAGE 첫 업로드면 isMain=true
            contentCommandService.uploadMedia(clubId, null, mainImage);
            uploadAllClubMedia(clubId, mediaFiles);
            return;
        }

        // mainImage 없이 mediaFiles만 온 경우: 기존 main(isMain=true) 유지
        MediaFile oldMain = mediaFileRepository
                .findTop1ByClub_IdAndPostIsNullAndIsMainTrueAndTypeIgnoreCaseOrderByIdAsc(clubId, "IMAGE")
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "MAIN_IMAGE_REQUIRED"));

        List<MediaFile> oldAll = mediaFileRepository.findByClub_IdAndPostIsNullOrderByIdAsc(clubId);

        List<MediaFile> toDelete = new ArrayList<>();
        for (MediaFile m : oldAll) {
            if (!m.getId().equals(oldMain.getId())) {
                toDelete.add(m);
            }
        }
        if (!toDelete.isEmpty()) mediaFileRepository.deleteAll(toDelete);

        uploadAllClubMedia(clubId, mediaFiles);
    }

    private void uploadAllClubMedia(Long clubId, List<MultipartFile> mediaFiles) {
        if (mediaFiles == null) return;
        for (MultipartFile f : mediaFiles) {
            if (f != null && !f.isEmpty()) {
                contentCommandService.uploadMedia(clubId, null, f);
            }
        }
    }

    private void assertClubAdminOrAdmin(SecurityUser me, Long clubId) {
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }
    }

    private boolean hasAnyFile(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return false;
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) return true;
        }
        return false;
    }

    private void validateRecruitPeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return;
        if (!end.isAfter(start)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "RECRUIT_END_MUST_BE_AFTER_START");
        }
    }

    private void requireImage(MultipartFile file, String code) {
        String ct = file.getContentType();
        if (!StringUtils.hasText(ct) || !ct.toLowerCase().startsWith("image/")) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
    }

    @PutMapping("/clubs/{clubId}/form")
    public UpsertFormResponse upsertForm(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @Valid @RequestBody FormUpsertRequest req
    ) {
        assertClubAdminOrAdmin(me, clubId);

        Long formId = formCommandService.upsertForm(clubId, req).getId();
        return new UpsertFormResponse(formId);
    }

    @GetMapping("/clubs/{clubId}/applications")
    public List<ApplicationListResponse> applications(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId
    ) {
        assertClubAdminOrAdmin(me, clubId);

        List<Application> apps = applicationAdminService.listByClub(clubId);

        return apps.stream().map(a -> new ApplicationListResponse(
                a.getId(),
                a.getUser().getId(),
                a.getUser().getStudentNo(),
                a.getUser().getName(),
                a.getUser().getDepartment(),
                a.getStatus().name(),
                a.getCreatedAt().toString()
        )).toList();
    }

    @GetMapping("/clubs/{clubId}/applications/{applicationId}")
    public ApplicationDetailResponse applicationDetail(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @PathVariable Long applicationId
    ) {
        assertClubAdminOrAdmin(me, clubId);

        Application app = applicationAdminService.get(applicationId);
        if (!app.getClub().getId().equals(clubId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "CLUB_ACCESS_DENIED");
        }

        List<ApplicationAnswer> answers = applicationAdminService.answers(applicationId);
        List<ApplicationDetailResponse.Answer> answerDtos = answers.stream().map(this::toAnswerDto).toList();

        return new ApplicationDetailResponse(
                app.getId(),
                app.getUser().getId(),
                app.getUser().getStudentNo(),
                app.getUser().getName(),
                app.getUser().getDepartment(),
                app.getStatus().name(),
                app.getCreatedAt().toString(),
                answerDtos
        );
    }

    private ApplicationDetailResponse.Answer toAnswerDto(ApplicationAnswer a) {
        FormQuestion q = a.getQuestion();

        Map<String, Object> payload = parsePayloadMap(q.getPayloadJson());

        List<String> words = payload.containsKey("words")
                ? objectMapper.convertValue(payload.get("words"), new TypeReference<List<String>>() {})
                : null;

        List<String> questions = payload.containsKey("questions")
                ? objectMapper.convertValue(payload.get("questions"), new TypeReference<List<String>>() {})
                : null;

        List<String> sentences = payload.containsKey("sentences")
                ? objectMapper.convertValue(payload.get("sentences"), new TypeReference<List<String>>() {})
                : null;

        ApplicationDetailResponse.TwoWordQuestions twoWordQuestions = payload.containsKey("twoWordQuestions")
                ? objectMapper.convertValue(payload.get("twoWordQuestions"), ApplicationDetailResponse.TwoWordQuestions.class)
                : null;

        ApplicationDetailResponse.Template5Questions template5Questions = payload.containsKey("template5Questions")
                ? objectMapper.convertValue(payload.get("template5Questions"), ApplicationDetailResponse.Template5Questions.class)
                : null;

        Object value = parseValueAny(a.getValueText());

        return new ApplicationDetailResponse.Answer(
                q.getOrderNo(),
                q.getTemplateNo(),
                q.getLabel(),
                words,
                questions,
                sentences,
                twoWordQuestions,
                template5Questions,
                value
        );
    }

    private Map<String, Object> parsePayloadMap(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_FORM_PAYLOAD_JSON");
        }
    }

    private Object parseValueAny(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return "";

        try {
            JsonNode node = objectMapper.readTree(s);
            return objectMapper.convertValue(node, Object.class);
        } catch (JsonProcessingException e) {
            return raw;
        }
    }

    @PatchMapping("/clubs/{clubId}/applications/{applicationId}/status")
    public void changeStatus(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @PathVariable Long applicationId,
            @Valid @RequestBody ChangeStatusRequest req
    ) {
        assertClubAdminOrAdmin(me, clubId);

        Application app = applicationAdminService.get(applicationId);
        if (!app.getClub().getId().equals(clubId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "CLUB_ACCESS_DENIED");
        }

        applicationAdminService.changeStatus(applicationId, req.status());
    }

    @GetMapping("/clubs/{clubId}/dashboard")
    public DashboardResponse dashboard(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId
    ) {
        assertClubAdminOrAdmin(me, clubId);

        ClubDashboardService.DashboardResult r = clubDashboardService.dashboard(clubId);
        return new DashboardResponse(
                r.totalApplications(),
                r.viewCount(),
                r.dailyApplications().stream()
                        .map(d -> new DashboardResponse.Daily(d.date(), d.count()))
                        .toList()
        );
    }
}
