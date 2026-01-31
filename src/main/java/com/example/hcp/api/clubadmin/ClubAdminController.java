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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubadmin")
@PreAuthorize("hasRole('CLUB_ADMIN') or hasRole('ADMIN')")
public class ClubAdminController {

    private final ClubAccessService clubAccessService;
    private final ClubCommandService clubCommandService;
    private final ContentCommandService contentCommandService;
    private final FormCommandService formCommandService;
    private final ApplicationAdminService applicationAdminService;
    private final ClubDashboardService clubDashboardService;
    private final ObjectMapper objectMapper;

    public ClubAdminController(
            ClubAccessService clubAccessService,
            ClubCommandService clubCommandService,
            ContentCommandService contentCommandService,
            FormCommandService formCommandService,
            ApplicationAdminService applicationAdminService,
            ClubDashboardService clubDashboardService,
            ObjectMapper objectMapper
    ) {
        this.clubAccessService = clubAccessService;
        this.clubCommandService = clubCommandService;
        this.contentCommandService = contentCommandService;
        this.formCommandService = formCommandService;
        this.applicationAdminService = applicationAdminService;
        this.clubDashboardService = clubDashboardService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/clubs")
    public MyClubsResponse myClubs(@AuthenticationPrincipal SecurityUser me) {
        return new MyClubsResponse(clubAccessService.myClubIds(me.userId()));
    }

    @PutMapping("/clubs/{clubId}")
    public void updateClub(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @Valid @RequestBody ClubUpsertRequest req
    ) {
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }

        Club changes = new Club();
        changes.setName(req.name());
        changes.setIntroduction(req.introduction());
        changes.setActivities(req.activities());
        changes.setRecruitTarget(req.recruitTarget());
        changes.setInterviewProcess(req.interviewProcess());
        changes.setContactLink(req.contactLink());
        changes.setCategory(req.category());
        changes.setRecruitmentStatus(req.recruitmentStatus());
        changes.setPublic(req.isPublic());

        clubCommandService.update(clubId, changes);
    }

    @PostMapping("/clubs/{clubId}/media")
    public UploadMediaResponse uploadMedia(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @RequestParam(required = false) Long postId,
            @RequestPart MultipartFile file
    ) {
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }
        MediaFile media = contentCommandService.uploadMedia(clubId, postId, file);
        return new UploadMediaResponse(media.getId());
    }

    @PutMapping("/clubs/{clubId}/form")
    public UpsertFormResponse upsertForm(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @Valid @RequestBody FormUpsertRequest req
    ) {
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }

        Long formId = formCommandService.upsertForm(clubId, req).getId();
        return new UpsertFormResponse(formId);
    }

    @GetMapping("/clubs/{clubId}/applications")
    public List<ApplicationListResponse> applications(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId
    ) {
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }

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
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }

        Application app = applicationAdminService.get(applicationId);
        if (!app.getClub().getId().equals(clubId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "CLUB_ACCESS_DENIED");
        }

        List<ApplicationAnswer> answers = applicationAdminService.answers(applicationId);

        // ✅ 질문(템플릿 필드 펼친 형태) + 지원자 답변(value) 같이 내려줌
        List<ApplicationDetailResponse.Answer> answerDtos =
                answers.stream().map(this::toAnswerDto).toList();

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
            return objectMapper.convertValue(node, Object.class); // Map/List/String/Number 등
        } catch (JsonProcessingException e) {
            // JSON이 아니면 문자열 그대로(텍스트 답변 호환)
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
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }

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
        if (!me.role().name().equals("ADMIN")) {
            clubAccessService.assertClubAdminAccess(me.userId(), clubId);
        }

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
