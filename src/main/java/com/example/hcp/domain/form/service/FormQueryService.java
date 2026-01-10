package com.example.hcp.domain.form.service;

import com.example.hcp.domain.form.entity.ApplicationForm;
import com.example.hcp.domain.form.entity.FormQuestion;
import com.example.hcp.domain.form.repository.ApplicationFormRepository;
import com.example.hcp.domain.form.repository.FormQuestionRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FormQueryService {

    private final ApplicationFormRepository formRepository;
    private final FormQuestionRepository questionRepository;

    public FormQueryService(ApplicationFormRepository formRepository, FormQuestionRepository questionRepository) {
        this.formRepository = formRepository;
        this.questionRepository = questionRepository;
    }

    public ApplicationForm getFormByClubId(Long clubId) {
        return formRepository.findByClub_Id(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "FORM_NOT_FOUND"));
    }

    public List<FormQuestion> questions(Long formId) {
        return questionRepository.findByForm_IdOrderByOrderNoAsc(formId);
    }
}
