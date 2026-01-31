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
        return formQueryService.questions(form.getId()); // orderNo ASC
    }

    @Transactional
    public Long submit(Long userId, Long clubId, List<JsonNode> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWERS");
        }
        for (JsonNode a : answers) {
            if (a == null || a.isNull()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWER");
            }
            // 텍스트 답변만 공백 방지(배열/객체는 템플릿별 검증에서 체크)
            if (a.isTextual() && a.asText().isBlank()) {
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

        // 템플릿별 검증
        for (int i = 0; i < questions.size(); i++) {
            validateAnswerByTemplate(questions.get(i), answers.get(i));
        }

        Application application = new Application();
        application.setUser(user);
        application.setClub(club);
        application.setForm(form);
        application.setStatus(ApplicationStatus.RECEIVED);
        application = applicationRepository.save(application);

        // 저장: 텍스트면 그대로, 배열/객체면 JSON 문자열로
        for (int i = 0; i < questions.size(); i++) {
            ApplicationAnswer ans = new ApplicationAnswer();
            ans.setApplication(application);
            ans.setQuestion(questions.get(i));
            ans.setValueText(toValueText(answers.get(i)));
            answerRepository.save(ans);
        }

        return application.getId();
    }

    public List<Application> myApplications(Long userId) {
        return applicationRepository.findByUser_IdOrderByIdDesc(userId);
    }

    private String toValueText(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.asText(); // template5 호환: "서술"
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_ANSWER_JSON");
        }
    }

    private void validateAnswerByTemplate(FormQuestion q, JsonNode ansNode) {
        int t = q.getTemplateNo();

        // (이전 폼 호환) templateNo=0: 문자열만
        if (t == 0) {
            if (ansNode == null || !ansNode.isTextual() || ansNode.asText().isBlank()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_ANSWER");
            }
            return;
        }

        switch (t) {
            case 1 -> { // ["단어1","단어2",...]
                List<String> allowed = payloadStringArray(q.getPayloadJson(), "words", "T1_OPTIONS_REQUIRED");
                List<String> chosen = requireStringArray(ansNode, "T1_ANSWER_MUST_BE_STRING_ARRAY");
                requireSubsetNoDup(chosen, allowed, "T1_INVALID_SELECTION");
                if (chosen.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T1_EMPTY_SELECTION");
            }

            case 2 -> { // [0,2,1] (질문 개수만큼)
                int questionCount = payloadStringArray(q.getPayloadJson(), "questions", "T2_QUESTIONS_REQUIRED").size();

                List<Integer> chosen = requireIntArray(ansNode, "T2_ANSWER_MUST_BE_INT_ARRAY");
                if (chosen.size() != questionCount) {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "T2_ANSWER_COUNT_MISMATCH");
                }
                for (Integer v : chosen) {
                    if (v == null || v < 0 || v > 3) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T2_INVALID_CHOICE_RANGE");
                    }
                }
            }

            case 3 -> { // ["문장1","문장3"]
                List<String> allowed = payloadStringArray(q.getPayloadJson(), "sentences", "T3_OPTIONS_REQUIRED");
                List<String> chosen = requireStringArray(ansNode, "T3_ANSWER_MUST_BE_STRING_ARRAY");
                requireSubsetNoDup(chosen, allowed, "T3_INVALID_SELECTION");
                if (chosen.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T3_EMPTY_SELECTION");
            }

            case 4 -> { // {"q1":["단어A"],"q2":["단어B"]} (문자열 1개도 허용)
                if (ansNode == null || !ansNode.isObject()) {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "T4_Q1_Q2_REQUIRED");
                }

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

            case 5 -> {
                // 허용:
                // 1) "서술"(텍스트만 = q2만)
                // 2) {"q2":"서술"} 또는 {"q1":{"number":2,"boolean":true},"q2":"서술"}
                if (ansNode == null) throw new ApiException(ErrorCode.BAD_REQUEST, "T5_INVALID_FORMAT");

                if (ansNode.isTextual()) {
                    if (ansNode.asText().isBlank()) throw new ApiException(ErrorCode.BAD_REQUEST, "T5_EMPTY_TEXT");
                    return;
                }

                if (!ansNode.isObject()) {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "T5_INVALID_FORMAT");
                }

                JsonNode q2 = ansNode.get("q2");
                if (q2 == null || !q2.isTextual() || q2.asText().isBlank()) {
                    throw new ApiException(ErrorCode.BAD_REQUEST, "T5_Q2_REQUIRED");
                }

                JsonNode q1 = ansNode.get("q1");
                if (q1 != null) {
                    if (!q1.isObject()) throw new ApiException(ErrorCode.BAD_REQUEST, "T5_INVALID_FORMAT");

                    JsonNode numNode = q1.get("number");
                    JsonNode boolNode = q1.get("boolean");

                    if (numNode == null || !numNode.isInt()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_Q1_NUMBER_REQUIRED");
                    }
                    if (boolNode == null || !boolNode.isBoolean()) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_Q1_BOOLEAN_REQUIRED");
                    }

                    int chosenNum = numNode.asInt();
                    boolean chosenBool = boolNode.asBoolean();

                    List<Integer> allowedNums = payloadIntArray(q.getPayloadJson(), "template5Questions.numberOptions", "T5_NUMBER_OPTIONS_REQUIRED");
                    List<Boolean> allowedBools = payloadBooleanArray(q.getPayloadJson(), "template5Questions.booleanOptions", "T5_BOOLEAN_OPTIONS_REQUIRED");

                    if (!allowedNums.contains(chosenNum)) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_INVALID_NUMBER_SELECTION");
                    }
                    if (!allowedBools.contains(chosenBool)) {
                        throw new ApiException(ErrorCode.BAD_REQUEST, "T5_INVALID_BOOLEAN_SELECTION");
                    }
                }
            }

            case 6 -> { // ["단어1","단어2",...]
                List<String> allowed = payloadStringArray(q.getPayloadJson(), "words", "T6_OPTIONS_REQUIRED");
                List<String> chosen = requireStringArray(ansNode, "T6_ANSWER_MUST_BE_STRING_ARRAY");
                requireSubsetNoDup(chosen, allowed, "T6_INVALID_SELECTION");
                if (chosen.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "T6_EMPTY_SELECTION");
            }

            default -> throw new ApiException(ErrorCode.BAD_REQUEST, "UNKNOWN_TEMPLATE_NO");
        }
    }

    private List<String> payloadStringArray(String payloadJson, String path, String errorCode) {
        JsonNode node = payloadNode(payloadJson, path, errorCode);
        return requireStringArray(node, errorCode);
    }

    private List<Integer> payloadIntArray(String payloadJson, String path, String errorCode) {
        JsonNode node = payloadNode(payloadJson, path, errorCode);
        return requireIntArray(node, errorCode);
    }

    private List<Boolean> payloadBooleanArray(String payloadJson, String path, String errorCode) {
        JsonNode node = payloadNode(payloadJson, path, errorCode);
        if (!node.isArray()) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        for (JsonNode n : node) {
            if (!n.isBoolean()) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        }
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Boolean.class)
        );
    }

    private JsonNode payloadNode(String payloadJson, String path, String errorCode) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode node = resolvePath(root, path);
            if (node == null || node.isNull() || node.isMissingNode()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
            }
            return node;
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
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
    }

    private List<Integer> requireIntArray(JsonNode node, String errorCode) {
        if (node == null || !node.isArray()) throw new ApiException(ErrorCode.BAD_REQUEST, errorCode);
        for (JsonNode n : node) {
            if (!n.isInt()) throw new ApiException(ErrorCode.BAD_REQUEST, "NON_INT_VALUE_IN_LIST");
        }
        return objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class)
        );
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
