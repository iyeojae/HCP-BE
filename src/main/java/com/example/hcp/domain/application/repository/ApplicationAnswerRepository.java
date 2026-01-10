package com.example.hcp.domain.application.repository;

import com.example.hcp.domain.application.entity.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
    List<ApplicationAnswer> findByApplication_IdOrderByIdAsc(Long applicationId);
    void deleteByApplication_Id(Long applicationId);
}
