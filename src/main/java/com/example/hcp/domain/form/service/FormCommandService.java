// src/main/java/com/example/hcp/domain/form/service/FormCommandService.java
package com.example.hcp.domain.form.service;

import com.example.hcp.api.clubadmin.request.FormUpsertRequest;
import com.example.hcp.domain.application.repository.ApplicationRepository;
import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.domain.form.entity.ApplicationForm;
import com.example.hcp.domain.form.entity.FormQuestion;
import com.example.hcp.domain.form.repository.ApplicationFormRepository;
import com.example.hcp.domain.form.repository.FormQuestionRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FormCommandService {

    private final ClubRepository clubRepository;
    private final ApplicationFormRepository formRepository;
    private final FormQuestionRepository questionRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    public FormCommandService(
            ClubRepository clubRepository,
            ApplicationFormRepository formRepository,
            FormQuestionRepository questionRepository,
            ApplicationRepository applicationRepository,
            ObjectMapper objectMapper
    ) {
        this.clubRepository = clubRepository;
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
        this.applicationRepository = applicationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * ✅ 변경점
     * - 기존: labels(List<String>)
     * - 현재: FormUpsertRequest(totalItems, items[{orderNo, templateNo, title, payload}])
     *
     * ※ FormQuestion은 다음 필드를 저장할 수 있어야 함:
     *   orderNo, templateNo, title(label), payloadJson(템플릿별 데이터)
     */
    @Transactional
    public ApplicationForm upsertForm(Long clubId, FormUpsertRequest req) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (applicationRepository.existsByClub_Id(clubId)) {
            throw new ApiException(ErrorCode.CONFLICT, "FORM_LOCKED_AFTER_APPLICATION");
        }

        validateUpsertRequest(req);

        ApplicationForm form = formRepository.findByClub_Id(clubId).orElseGet(() -> {
            ApplicationForm f = new ApplicationForm();
            f.setClub(club);
            return formRepository.save(f);
        });

        // 폼 메타 저장(총 블록 수)
        form.setItemCount(req.totalItems());

        // 기존 질문(블록) 전부 교체
        questionRepository.deleteByForm_Id(form.getId());

        List<FormUpsertRequest.Item> sorted = req.items().stream()
                .sorted(Comparator.comparingInt(FormUpsertRequest.Item::orderNo))
                .toList();

        for (FormUpsertRequest.Item item : sorted) {
            String payloadJson = toJson(item.payload());

            // ✅ FormQuestion 생성자는 아래 형태로 맞춰야 함(엔티티 수정 필요):
            // new FormQuestion(form, orderNo, templateNo, title, payloadJson)
            FormQuestion q = new FormQuestion(
                    form,
                    item.orderNo(),
                    item.templateNo(),
                    item.title(),
                    payloadJson
            );
            questionRepository.save(q);
        }

        return form;
    }

    private void validateUpsertRequest(FormUpsertRequest req) {
        if (req == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "FORM_REQUEST_REQUIRED");
        }
        if (req.totalItems() == null || req.totalItems() <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_TOTAL_ITEMS");
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ITEMS");
        }
        if (req.totalItems() != req.items().size()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "TOTAL_ITEMS_MISMATCH");
        }

        // orderNo 유니크 + 1..N 연속 보장
        Set<Integer> seen = new HashSet<>();
        for (FormUpsertRequest.Item item : req.items()) {
            if (item == null) throw new ApiException(ErrorCode.BAD_REQUEST, "NULL_ITEM");
            if (item.orderNo() == null || item.orderNo() <= 0) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_ORDER_NO");
            }
            if (!seen.add(item.orderNo())) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "DUPLICATE_ORDER_NO");
            }
        }
        int n = req.totalItems();
        for (int i = 1; i <= n; i++) {
            if (!seen.contains(i)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "ORDER_NO_NOT_CONTIGUOUS");
            }
        }

        // 템플릿별 payload 구조 검증(개수 규칙만 강제)
        for (FormUpsertRequest.Item item : req.items()) {
            if (item.templateNo() == null || item.templateNo() < 1 || item.templateNo() > 6) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_TEMPLATE_NO");
            }
            if (item.title() == null || item.title().isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_TITLE");
            }

            FormUpsertRequest.Payload p = item.payload();
            switch (item.templateNo()) {
                case 1 -> { // 단어 11개
                    requireListSize(p == null ? null : p.words(), 11, "T1_WORDS_11_REQUIRED");
                }
                case 2 -> { // 질문 3개
                    requireListSize(p == null ? null : p.questions(), 3, "T2_QUESTIONS_3_REQUIRED");
                }
                case 3 -> { // 문장 4개
                    requireListSize(p == null ? null : p.sentences(), 4, "T3_SENTENCES_4_REQUIRED");
                }
                case 4 -> { // 질문2개(단어4 + 단어3)
                    if (p == null || p.twoWordQuestions() == null) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T4_PAYLOAD_REQUIRED");
                    }
                    var tw = p.twoWordQuestions();
                    if (tw.question1Title() == null || tw.question1Title().isBlank()
                            || tw.question2Title() == null || tw.question2Title().isBlank()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T4_TITLES_REQUIRED");
                    }
                    requireListSize(tw.question1Words(), 4, "T4_Q1_WORDS_4_REQUIRED");
                    requireListSize(tw.question2Words(), 3, "T4_Q2_WORDS_3_REQUIRED");
                }
                case 5 -> { // 질문 2개(자유서술용)
                    if (p == null || p.twoTextQuestions() == null) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_PAYLOAD_REQUIRED");
                    }
                    var tt = p.twoTextQuestions();
                    if (tt.question1Title() == null || tt.question1Title().isBlank()
                            || tt.question2Title() == null || tt.question2Title().isBlank()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_TITLES_REQUIRED");
                    }
                }
                case 6 -> { // 단어 8개
                    requireListSize(p == null ? null : p.words(), 8, "T6_WORDS_8_REQUIRED");
                }
            }
        }
    }

    private void requireListSize(List<String> list, int size, String code) {
        if (list == null || list.size() != size) {
            throw new ApiException(ErrorCode.BAD_REQUEST, code);
        }
        for (String v : list) {
            if (v == null || v.isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_VALUE_IN_LIST");
            }
        }
    }

    private String toJson(Object payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_PAYLOAD_JSON");
        }
    }
}
