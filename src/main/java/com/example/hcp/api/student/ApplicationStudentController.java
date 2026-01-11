// src/main/java/com/example/hcp/api/student/ApplicationStudentController.java
package com.example.hcp.api.student;

import com.example.hcp.domain.application.entity.Application;
import com.example.hcp.domain.application.service.ApplicationStudentService;
import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.domain.form.entity.ApplicationForm;
import com.example.hcp.domain.form.entity.FormQuestion;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.example.hcp.global.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class ApplicationStudentController {

    private final ApplicationStudentService applicationStudentService;
    private final ClubRepository clubRepository;

    public ApplicationStudentController(ApplicationStudentService applicationStudentService, ClubRepository clubRepository) {
        this.applicationStudentService = applicationStudentService;
        this.clubRepository = clubRepository;
    }

    @GetMapping("/clubs/{clubId}/form")
    public FormResponse form(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId
    ) {
        ApplicationForm form = applicationStudentService.form(clubId);
        List<FormQuestion> qs = applicationStudentService.formQuestions(clubId);

        List<String> labels = qs.stream()
                .map(FormQuestion::getLabel)
                .toList();

        return new FormResponse(form.getId(), clubId, labels);
    }

    @PostMapping("/clubs/{clubId}/applications")
    public SubmitApplicationResponse submit(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId,
            @Valid @RequestBody SubmitApplicationRequest req
    ) {
        Long appId = applicationStudentService.submit(me.userId(), clubId, req.answers());
        return new SubmitApplicationResponse(appId);
    }

    // ✅ 변경: 래퍼(MyApplicationsResponse)로 감싸지 않고 List를 그대로 반환
    @GetMapping("/applications")
    public List<MyApplicationsResponse.Item> my(@AuthenticationPrincipal SecurityUser me) {
        List<Application> apps = applicationStudentService.myApplications(me.userId());

        return apps.stream().map(a -> {
            Club c = clubRepository.findById(a.getClub().getId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));
            return new MyApplicationsResponse.Item(
                    a.getId(),
                    c.getId(),
                    c.getName(),
                    a.getStatus().name(),
                    a.getCreatedAt().toString()
            );
        }).toList();
    }
}
