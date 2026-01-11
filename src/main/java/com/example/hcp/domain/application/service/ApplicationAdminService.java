// src/main/java/com/example/hcp/domain/application/service/ApplicationAdminService.java
package com.example.hcp.domain.application.service;

import com.example.hcp.domain.application.entity.Application;
import com.example.hcp.domain.application.entity.ApplicationAnswer;
import com.example.hcp.domain.application.entity.ApplicationStatus;
import com.example.hcp.domain.application.repository.ApplicationAnswerRepository;
import com.example.hcp.domain.application.repository.ApplicationRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationAdminService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository answerRepository;

    public ApplicationAdminService(ApplicationRepository applicationRepository, ApplicationAnswerRepository answerRepository) {
        this.applicationRepository = applicationRepository;
        this.answerRepository = answerRepository;
    }

    public List<Application> listByClub(Long clubId) {
        // ✅ 변경: user 미리 로딩된 조회 사용
        return applicationRepository.findWithUserByClub_IdOrderByIdDesc(clubId);
    }

    public Application get(Long applicationId) {
        // ✅ 변경: user+club 미리 로딩된 조회 사용
        return applicationRepository.findWithUserAndClubById(applicationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "APPLICATION_NOT_FOUND"));
    }

    public List<ApplicationAnswer> answers(Long applicationId) {
        // ✅ 변경: question 미리 로딩된 조회 사용
        return answerRepository.findWithQuestionByApplication_IdOrderByIdAsc(applicationId);
    }

    @Transactional
    public void changeStatus(Long applicationId, ApplicationStatus status) {
        Application app = get(applicationId);
        app.setStatus(status);
    }
}
