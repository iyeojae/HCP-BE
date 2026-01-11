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
import com.example.hcp.domain.form.service.FormCommandService;
import com.example.hcp.domain.stats.service.ClubDashboardService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.example.hcp.global.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    public ClubAdminController(
            ClubAccessService clubAccessService,
            ClubCommandService clubCommandService,
            ContentCommandService contentCommandService,
            FormCommandService formCommandService,
            ApplicationAdminService applicationAdminService,
            ClubDashboardService clubDashboardService
    ) {
        this.clubAccessService = clubAccessService;
        this.clubCommandService = clubCommandService;
        this.contentCommandService = contentCommandService;
        this.formCommandService = formCommandService;
        this.applicationAdminService = applicationAdminService;
        this.clubDashboardService = clubDashboardService;
    }

    @GetMapping("/clubs")
    public MyClubsResponse myClubs(@AuthenticationPrincipal SecurityUser me) {
        return new MyClubsResponse(clubAccessService.myClubIds(me.userId()));
    }

    // ✅ 수정은 CLUB_ADMIN(자기 동아리만) + ADMIN
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
            @RequestParam(required = false) Long postId, // posts 없애면 프론트에서는 그냥 안 보내면 됨(null)
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

        Long formId = formCommandService.upsertForm(clubId, req.labels()).getId();
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

        List<ApplicationDetailResponse.Answer> answerDtos = answers.stream().map(a ->
                new ApplicationDetailResponse.Answer(
                        a.getQuestion().getLabel(),
                        a.getValueText()
                )
        ).toList();

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
