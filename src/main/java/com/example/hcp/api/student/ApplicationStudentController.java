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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class ApplicationStudentController {

    private final ApplicationStudentService applicationStudentService;
    private final ClubRepository clubRepository;
    private final ObjectMapper objectMapper;

    public ApplicationStudentController(
            ApplicationStudentService applicationStudentService,
            ClubRepository clubRepository,
            ObjectMapper objectMapper
    ) {
        this.applicationStudentService = applicationStudentService;
        this.clubRepository = clubRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/clubs/{clubId}/form")
    public FormResponse form(
            @AuthenticationPrincipal SecurityUser me,
            @PathVariable Long clubId
    ) {
        ApplicationForm form = applicationStudentService.form(clubId);
        List<FormQuestion> qs = applicationStudentService.formQuestions(clubId);

        List<FormResponse.Item> items = qs.stream()
                .map(q -> new FormResponse.Item(
                        q.getOrderNo(),
                        q.getTemplateNo(),
                        q.getLabel(),
                        parsePayloadObject(q.getPayloadJson())
                ))
                .toList();

        int totalItems = form.getItemCount() > 0 ? form.getItemCount() : items.size();
        return new FormResponse(totalItems, items);
    }

    private Object parsePayloadObject(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            // Map/List로 파싱되므로 그대로 JSON으로 깔끔하게 직렬화됨
            return objectMapper.readValue(payloadJson, Object.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_PAYLOAD_JSON");
        }
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
