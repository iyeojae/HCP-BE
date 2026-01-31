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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
                .map(this::toResponseItem)
                .toList();

        // ✅ 요청과 동일 의미로 totalItems는 items.size()로 고정
        return new FormResponse(items.size(), items);
    }

    private FormResponse.Item toResponseItem(FormQuestion q) {
        Map<String, Object> payload = parsePayloadMap(q.getPayloadJson());
        int t = q.getTemplateNo();

        List<String> words = null;
        List<String> questions = null;
        List<String> sentences = null;
        FormResponse.TwoWordQuestions twoWordQuestions = null;
        FormResponse.Template5Questions template5Questions = null;

        // ✅ templateNo 기준으로 필요한 값만 꺼냄 (응답이 요청 형태처럼 깔끔해지고, 불필요한 변환 제거)
        switch (t) {
            case 1, 6 -> words = asStringList(payload.get("words"));
            case 2 -> questions = asStringList(payload.get("questions"));
            case 3 -> sentences = asStringList(payload.get("sentences"));
            case 4 -> {
                Object v = payload.get("twoWordQuestions");
                twoWordQuestions = (v == null) ? null : objectMapper.convertValue(v, FormResponse.TwoWordQuestions.class);
            }
            case 5 -> {
                Object v = payload.get("template5Questions");
                template5Questions = (v == null) ? null : objectMapper.convertValue(v, FormResponse.Template5Questions.class);
            }
        }

        return new FormResponse.Item(
                q.getOrderNo(),
                q.getTemplateNo(),
                q.getLabel(),
                words,
                questions,
                sentences,
                twoWordQuestions,
                template5Questions
        );
    }

    private List<String> asStringList(Object v) {
        if (v == null) return null;
        return objectMapper.convertValue(v, new TypeReference<List<String>>() {});
    }

    private Map<String, Object> parsePayloadMap(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
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
