// src/main/java/com/example/hcp/domain/form/service/FormCommandService.java
package com.example.hcp.domain.form.service;

import com.example.hcp.domain.application.repository.ApplicationRepository;
import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.domain.form.entity.ApplicationForm;
import com.example.hcp.domain.form.entity.FormQuestion;
import com.example.hcp.domain.form.repository.ApplicationFormRepository;
import com.example.hcp.domain.form.repository.FormQuestionRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FormCommandService {

    private final ClubRepository clubRepository;
    private final ApplicationFormRepository formRepository;
    private final FormQuestionRepository questionRepository;
    private final ApplicationRepository applicationRepository;

    public FormCommandService(
            ClubRepository clubRepository,
            ApplicationFormRepository formRepository,
            FormQuestionRepository questionRepository,
            ApplicationRepository applicationRepository
    ) {
        this.clubRepository = clubRepository;
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplicationForm upsertForm(Long clubId, List<String> labels) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (applicationRepository.existsByClub_Id(clubId)) {
            throw new ApiException(ErrorCode.CONFLICT, "FORM_LOCKED_AFTER_APPLICATION");
        }

        ApplicationForm form = formRepository.findByClub_Id(clubId).orElseGet(() -> {
            ApplicationForm f = new ApplicationForm();
            f.setClub(club);
            return formRepository.save(f);
        });

        questionRepository.deleteByForm_Id(form.getId());

        int orderNo = 1;
        for (String label : labels) {
            FormQuestion q = new FormQuestion();
            q.setForm(form);
            q.setOrderNo(orderNo++);
            q.setLabel(label);

            // DB 제약 대응용 고정값
            q.setType("TEXT");
            q.setRequired(true);
            q.setOptionsJson(null);

            questionRepository.save(q);
        }

        return form;
    }
}
