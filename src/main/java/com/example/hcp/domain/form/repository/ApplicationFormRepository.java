package com.example.hcp.domain.form.repository;

import com.example.hcp.domain.form.entity.ApplicationForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationFormRepository extends JpaRepository<ApplicationForm, Long> {
    Optional<ApplicationForm> findByClub_Id(Long clubId);
}
