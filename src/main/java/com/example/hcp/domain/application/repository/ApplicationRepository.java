package com.example.hcp.domain.application.repository;

import com.example.hcp.domain.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUser_IdOrderByIdDesc(Long userId);
    List<Application> findByClub_IdOrderByIdDesc(Long clubId);

    boolean existsByUser_IdAndClub_Id(Long userId, Long clubId);

    // FormCommandService에서 폼 수정 잠금에 사용
    boolean existsByClub_Id(Long clubId);
}
