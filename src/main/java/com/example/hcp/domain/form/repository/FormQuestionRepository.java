// src/main/java/com/example/hcp/domain/form/repository/FormQuestionRepository.java
package com.example.hcp.domain.form.repository;

import com.example.hcp.domain.form.entity.FormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormQuestionRepository extends JpaRepository<FormQuestion, Long> {
    List<FormQuestion> findByForm_IdOrderByOrderNoAsc(Long formId);
    void deleteByForm_Id(Long formId);
}
