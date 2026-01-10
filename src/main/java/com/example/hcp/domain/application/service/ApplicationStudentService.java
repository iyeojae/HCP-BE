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
import com.example.hcp.domain.form.repository.FormQuestionRepository;
import com.example.hcp.domain.form.service.FormQueryService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationStudentService {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final FormQueryService formQueryService;
    private final FormQuestionRepository questionRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository answerRepository;

    public ApplicationStudentService(
            UserRepository userRepository,
            ClubRepository clubRepository,
            FormQueryService formQueryService,
            FormQuestionRepository questionRepository,
            ApplicationRepository applicationRepository,
            ApplicationAnswerRepository answerRepository
    ) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.formQueryService = formQueryService;
        this.questionRepository = questionRepository;
        this.applicationRepository = applicationRepository;
        this.answerRepository = answerRepository;
    }

    public ApplicationForm form(Long clubId) {
        return formQueryService.getFormByClubId(clubId);
    }

    public List<FormQuestion> formQuestions(Long clubId) {
        ApplicationForm form = form(clubId);
        return questionRepository.findByForm_IdOrderByOrderNoAsc(form.getId());
    }

    @Transactional
    public Long submit(Long userId, Long clubId, List<AnswerInput> answers) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (!club.isPublic()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND");
        }

        ApplicationForm form = formQueryService.getFormByClubId(clubId);
        List<FormQuestion> questions = questionRepository.findByForm_IdOrderByOrderNoAsc(form.getId());
        Map<Long, FormQuestion> qMap = questions.stream().collect(Collectors.toMap(FormQuestion::getId, q -> q));

        Application application = new Application();
        application.setUser(user);
        application.setClub(club);
        application.setForm(form);
        application.setStatus(ApplicationStatus.RECEIVED);

        application = applicationRepository.save(application);

        for (AnswerInput in : answers) {
            FormQuestion q = qMap.get(in.questionId());
            if (q == null) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_QUESTION_ID");
            }

            ApplicationAnswer a = new ApplicationAnswer();
            a.setApplication(application);
            a.setQuestion(q);
            a.setValueText(in.value());
            answerRepository.save(a);
        }

        return application.getId();
    }

    public List<Application> myApplications(Long userId) {
        return applicationRepository.findByUser_IdOrderByIdDesc(userId);
    }

    public record AnswerInput(Long questionId, String value) {}
}
