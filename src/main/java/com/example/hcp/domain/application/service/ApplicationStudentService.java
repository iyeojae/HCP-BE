// src/main/java/com/example/hcp/domain/application/service/ApplicationStudentService.java
package com.example.hcp.domain.application.service;

import com.example.hcp.domain.account.entity.User;
import com.example.hcp.domain.account.repository.UserRepository;
import com.example.hcp.domain.application.entity.Application;
import com.example.hcp.domain.application.entity.ApplicationAnswer;
import com.example.hcp.domain.application.entity.ApplicationStatus;
import com.example.hcp.domain.application.repository.ApplicationAnswerRepository;
import com.example.hcp.domain.application.repository.ApplicationRepository;
import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.domain.form.entity.ApplicationForm;
import com.example.hcp.domain.form.entity.FormQuestion;
import com.example.hcp.domain.form.service.FormQueryService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ApplicationStudentService {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final FormQueryService formQueryService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    public ApplicationStudentService(
            UserRepository userRepository,
            ClubRepository clubRepository,
            FormQueryService formQueryService,
            ApplicationRepository applicationRepository,
            ApplicationAnswerRepository answerRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.formQueryService = formQueryService;
        this.applicationRepository = applicationRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
    }

    public ApplicationForm form(Long clubId) {
        return formQueryService.getFormByClubId(clubId);
    }

    public List<FormQuestion> formQuestions(Long clubId) {
        ApplicationForm form = form(clubId);
        return formQueryService.questions(form.getId());
    }

    @Transactional
    public Long submit(Long userId, Long clubId, List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWERS");
        }
        for (String a : answers) {
            if (a == null || a.isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWER");
            }
        }

        if (applicationRepository.existsByUser_IdAndClub_Id(userId, clubId)) {
            throw new ApiException(ErrorCode.CONFLICT, "ALREADY_APPLIED");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (!club.isPublic()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND");
        }

        ApplicationForm form = formQueryService.getFormByClubId(clubId);
        List<FormQuestion> questions = formQueryService.questions(form.getId());

        if (questions.size() != answers.size()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "ANSWER_COUNT_MISMATCH");
        }

        // ✅ 템플릿별 답변 포맷 검증 (순서는 orderNo 조회 정렬 결과 기준)
        for (int i = 0; i < questions.size(); i++) {
            validateAnswerByTemplate(questions.get(i), answers.get(i));
        }

        Application application = new Application();
        application.setUser(user);
        application.setClub(club);
        application.setForm(form);
        application.setStatus(ApplicationStatus.RECEIVED);
        application = applicationRepository.save(application);

        for (int i = 0; i < questions.size(); i++) {
            ApplicationAnswer ans = new ApplicationAnswer();
            ans.setApplication(application);
            ans.setQuestion(questions.get(i));
            ans.setValueText(answers.get(i)); // ✅ 템플릿별 JSON(또는 텍스트)을 그대로 저장
            answerRepository.save(ans);
        }

        return application.getId();
    }

    public List<Application> myApplications(Long userId) {
        return applicationRepository.findByUser_IdOrderByIdDesc(userId);
    }

    private void validateAnswerByTemplate(FormQuestion q, String rawAnswer) {
        int t = q.getTemplateNo();

        // (이전 폼 호환) templateNo가 0이면 기존처럼 "문자열"만 허용
        if (t == 0) {
            if (rawAnswer.isBlank()) throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWER");
            return;
        }

        // template 1,2,3,4,6은 JSON 파싱 필수
        // template 5는 JSON object 또는 plain text(=2번 질문 답변만) 둘 다 허용
        JsonNode ansNode = null;
        try {
            ansNode = objectMapper.readTree(rawAnswer);
        } catch (JsonProcessingException e) {
            if (t == 5) {
                if (rawAnswer.isBlank()) throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWER");
                return;
            }
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_ANSWER_JSON");
        }

        switch (t) {
            case 1 -> { // 단어 11개 중 선택(선택된 단어들만 전송): ["단어1","단어2",...]
                List<String> allowed = payloadStringArray(q.getPayloadJson(), "words", "T1_OPTIONS_REQUIRED");
                List<String> chosen = requireStringArray(ansNode, "T1_ANSWER_MUST_BE_STRING_ARRAY");
                requireSubsetNoDup(chosen, allowed, "T1_INVALID_SELECTION");
                if (chosen.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T1_EMPTY_SELECTION");
            }
            case 2 -> { // 질문 3개에 대해 0~3 선택: [0,2,1]
                List<Integer> chosen = requireIntArray(ansNode, "T2_ANSWER_MUST_BE_INT_ARRAY");
                if (chosen.size() != 3) throw new ApiException(ErrorCode.BAD_REQUEST, "T2_ANSWER_COUNT_MUST_BE_3");
                for (Integer v : chosen) {
                    if (v == null || v < 0 || v > 3) throw new ApiException(ErrorCode.BAD_REQUEST, "T2_INVALID_CHOICE_RANGE");
                }
            }
            case 3 -> { // 문장 4개 중 선택(선택 문장만 전송): ["문장1"] 또는 ["문장1","문장3"]
                List<String> allowed = payloadStringArray(q.getPayloadJson(), "sentences", "T3_OPTIONS_REQUIRED");
                List<String> chosen = requireStringArray(ansNode, "T3_ANSWER_MUST_BE_STRING_ARRAY");
                requireSubsetNoDup(chosen, allowed, "T3_INVALID_SELECTION");
                if (chosen.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T3_EMPTY_SELECTION");
            }
            case 4 -> { // 2개 질문(각 질문 단어 선택): {"q1":["단어A"],"q2":["단어B"]}
                JsonNode q1Node = ansNode.get("q1");
                JsonNode q2Node = ansNode.get("q2");
                if (q1Node == null || q2Node == null) throw new ApiException(ErrorCode.BAD_REQUEST, "T4_Q1_Q2_REQUIRED");

                List<String> allowedQ1 = payloadStringArray(q.getPayloadJson(), "twoWordQuestions.question1Words", "T4_Q1_OPTIONS_REQUIRED");
                List<String> allowedQ2 = payloadStringArray(q.getPayloadJson(), "twoWordQuestions.question2Words", "T4_Q2_OPTIONS_REQUIRED");

                List<String> chosenQ1 = nodeToStringList(q1Node, "T4_Q1_INVALID_FORMAT");
                List<String> chosenQ2 = nodeToStringList(q2Node, "T4_Q2_INVALID_FORMAT");

                requireSubsetNoDup(chosenQ1, allowedQ1, "T4_Q1_INVALID_SELECTION");
                requireSubsetNoDup(chosenQ2, allowedQ2, "T4_Q2_INVALID_SELECTION");

                if (chosenQ1.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T4_Q1_EMPTY_SELECTION");
                if (chosenQ2.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T4_Q2_EMPTY_SELECTION");
            }
            case 5 -> { // 질문 2개(2번 질문은 자유서술): {"q1":"...","q2":"..."} 또는 "..."
                if (ansNode.isObject()) {
                    JsonNode q2 = ansNode.get("q2");
                    if (q2 == null || !q2.isTextual() || q2.asText().isBlank()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_Q2_REQUIRED");
                    }
                } else if (ansNode.isTextual()) {
                    if (ansNode.asText().isBlank()) throw new ApiException(ErrorCode.BAD_REQUEST, "T5_EMPTY_TEXT");
                } else {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "T5_INVALID_FORMAT");
                }
            }
            case 6 -> { // 단어 8개 중 선택(선택된 단어들만 전송): ["단어1","단어2",...]
                List<String> allowed = payloadStringArray(q.getPayloadJson(), "words", "T6_OPTIONS_REQUIRED");
                List<String> chosen = requireStringArray(ansNode, "T6_ANSWER_MUST_BE_STRING_ARRAY");
                requireSubsetNoDup(chosen, allowed, "T6_INVALID_SELECTION");
                if (chosen.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T6_EMPTY_SELECTION");
            }
            default -> throw new ApiException(ErrorCode.BAD_REQUEST, "UNKNOWN_TEMPLATE_NO");
        }
    }

    private List<String> payloadStringArray(String payloadJson, String path, String errorCode) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode node = resolvePath(root, path);
            return requireStringArray(node, errorCode);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_FORM_PAYLOAD_JSON");
        }
    }

    private JsonNode resolvePath(JsonNode root, String path) {
        JsonNode cur = root;
        for (String p : path.split("\\.")) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        return cur;
    }

    private List<String> requireStringArray(JsonNode node, String errorCode) {
        if (node == null || !node.isArray()) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        for (JsonNode n : node) {
            if (!n.isTextual() || n.asText().isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_VALUE_IN_LIST");
            }
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private List<Integer> requireIntArray(JsonNode node, String errorCode) {
        if (node == null || !node.isArray()) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        for (JsonNode n : node) {
            if (!n.isInt()) throw new ApiException(ErrorCode.BAD_REQUEST, "NON_INT_VALUE_IN_LIST");
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
    }

    private List<String> nodeToStringList(JsonNode node, String errorCode) {
        if (node == null) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);

        if (node.isTextual()) {
            String v = node.asText();
            if (v.isBlank()) throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_VALUE");
            return List.of(v);
        }

        if (node.isArray()) {
            return requireStringArray(node, errorCode);
        }

        throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
    }

    private void requireSubsetNoDup(List<String> chosen, List<String> allowed, String errorCode) {
        Set<String> allowedSet = new HashSet<>(allowed);
        Set<String> seen = new HashSet<>();
        for (String c : chosen) {
            if (!allowedSet.contains(c)) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
            if (!seen.add(c)) throw new ApiException(ErrorCode.BAD_REQUEST, "DUPLICATE_SELECTION");
        }
    }
}
