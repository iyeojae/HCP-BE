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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationStudentService {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final FormQueryService formQueryService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository answerRepository;

    public ApplicationStudentService(
            UserRepository userRepository,
            ClubRepository clubRepository,
            FormQueryService formQueryService,
            ApplicationRepository applicationRepository,
            ApplicationAnswerRepository answerRepository
    ) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.formQueryService = formQueryService;
        this.applicationRepository = applicationRepository;
        this.answerRepository = answerRepository;
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
        if (answers.size() > 50) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "TOO_MANY_ANSWERS");
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
            ans.setValueText(answers.get(i));
            answerRepository.save(ans);
        }

        return application.getId();
    }

    public List<Application> myApplications(Long userId) {
        return applicationRepository.findByUser_IdOrderByIdDesc(userId);
    }
}
