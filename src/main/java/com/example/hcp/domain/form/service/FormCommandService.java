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

    @Transactional
    public ApplicationForm upsertForm(Long clubId, FormUpsertRequest req) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (applicationRepository.existsByClub_Id(clubId)) {
            throw new ApiException(ErrorCode.CONFLICT, "FORM_LOCKED_AFTER_APPLICATION");
        }

        validateUpsertRequest(req);

        // ✅ 변경: protected 생성자 대신 팩토리 사용
        ApplicationForm form = formRepository.findByClub_Id(clubId)
                .orElseGet(() -> formRepository.save(ApplicationForm.create(club)));

        // 메타 저장
        form.setItemCount(req.totalItems());

        // 기존 질문 전부 교체
        questionRepository.deleteByForm_Id(form.getId());

        List<FormUpsertRequest.Item> sorted = req.items().stream()
                .sorted(Comparator.comparingInt(FormUpsertRequest.Item::orderNo))
                .toList();

        for (FormUpsertRequest.Item item : sorted) {
            String payloadJson = toJson(item.payload());

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
        if (req == null) throw new ApiException(ErrorCode.BAD_REQUEST, "FORM_REQUEST_REQUIRED");
        if (req.totalItems() == null || req.totalItems() <= 0) throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_TOTAL_ITEMS");
        if (req.items() == null || req.items().isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ITEMS");
        if (!req.totalItems().equals(req.items().size())) throw new ApiException(ErrorCode.BAD_REQUEST, "TOTAL_ITEMS_MISMATCH");

        // orderNo 유니크 + 1..N 연속
        Set<Integer> seen = new HashSet<>();
        for (FormUpsertRequest.Item item : req.items()) {
            if (item == null) throw new ApiException(ErrorCode.BAD_REQUEST, "NULL_ITEM");
            if (item.orderNo() == null || item.orderNo() <= 0) throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_ORDER_NO");
            if (!seen.add(item.orderNo())) throw new ApiException(ErrorCode.BAD_REQUEST, "DUPLICATE_ORDER_NO");
        }
        for (int i = 1; i <= req.totalItems(); i++) {
            if (!seen.contains(i)) throw new ApiException(ErrorCode.BAD_REQUEST, "ORDER_NO_NOT_CONTIGUOUS");
        }

        // 템플릿별 payload 검증
        for (FormUpsertRequest.Item item : req.items()) {
            if (item.templateNo() == null || item.templateNo() < 1 || item.templateNo() > 6) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_TEMPLATE_NO");
            }
            if (item.title() == null || item.title().isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_TITLE");
            }
            if (item.payload() == null) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "PAYLOAD_REQUIRED");
            }

            FormUpsertRequest.Payload p = item.payload();

            switch (item.templateNo()) {
                case 1 -> requireStringListSize(p.words(), 11, "T1_WORDS_11_REQUIRED");
                case 2 -> requireStringListSize(p.questions(), 3, "T2_QUESTIONS_3_REQUIRED");
                case 3 -> requireStringListSize(p.sentences(), 4, "T3_SENTENCES_4_REQUIRED");
                case 4 -> {
                    if (p.twoWordQuestions() == null) throw new ApiException(ErrorCode.BAD_REQUEST, "T4_PAYLOAD_REQUIRED");
                    var tw = p.twoWordQuestions();
                    if (tw.question1Title() == null || tw.question1Title().isBlank()
                            || tw.question2Title() == null || tw.question2Title().isBlank()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T4_TITLES_REQUIRED");
                    }
                    requireStringListSize(tw.question1Words(), 4, "T4_Q1_WORDS_4_REQUIRED");
                    requireStringListSize(tw.question2Words(), 3, "T4_Q2_WORDS_3_REQUIRED");
                }
                case 5 -> {
                    // ✅ 템플릿5: 2개 질문
                    // - 1번 질문: number(0~3) + boolean(true/false) 선택형
                    // - 2번 질문: 사용자가 입력(문항 제목만 필요)
                    if (p.template5Questions() == null) throw new ApiException(ErrorCode.BAD_REQUEST, "T5_PAYLOAD_REQUIRED");
                    var t5 = p.template5Questions();

                    if (t5.question1Title() == null || t5.question1Title().isBlank()
                            || t5.question2Title() == null || t5.question2Title().isBlank()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_TITLES_REQUIRED");
                    }

                    // number 옵션은 [0,1,2,3] 형태 강제(원하면 규칙 바꿔도 됨)
                    requireIntListExact(t5.numberOptions(), List.of(0, 1, 2, 3), "T5_NUMBER_OPTIONS_REQUIRED");

                    // boolean 옵션은 [true,false] 포함 강제
                    requireBooleanOptions(t5.booleanOptions(), "T5_BOOLEAN_OPTIONS_REQUIRED");
                }
                case 6 -> requireStringListSize(p.words(), 8, "T6_WORDS_8_REQUIRED");
            }
        }
    }

    private void requireStringListSize(List<String> list, int size, String code) {
        if (list == null || list.size() != size) throw new ApiException(ErrorCode.BAD_REQUEST, code);
        for (String v : list) if (v == null || v.isBlank()) throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_VALUE_IN_LIST");
    }

    private void requireIntListExact(List<Integer> list, List<Integer> expected, String code) {
        if (list == null || list.size() != expected.size()) throw new ApiException(ErrorCode.BAD_REQUEST, code);
        for (int i = 0; i < expected.size(); i++) {
            if (list.get(i) == null || !list.get(i).equals(expected.get(i))) {
                throw new ApiException(ErrorCode.BAD_REQUEST, code);
            }
        }
    }

    private void requireBooleanOptions(List<Boolean> list, String code) {
        if (list == null || list.size() != 2) throw new ApiException(ErrorCode.BAD_REQUEST, code);
        boolean hasTrue = false, hasFalse = false;
        for (Boolean b : list) {
            if (b == null) throw new ApiException(ErrorCode.BAD_REQUEST, code);
            if (b) hasTrue = true; else hasFalse = true;
        }
        if (!hasTrue || !hasFalse) throw new ApiException(ErrorCode.BAD_REQUEST, code);
    }

    private String toJson(Object payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_PAYLOAD_JSON");
        }
    }
}
