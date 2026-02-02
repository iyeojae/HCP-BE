// src/main/java/com/example/hcp/domain/application/repository/ApplicationRepository.java
package com.example.hcp.domain.application.repository;

import com.example.hcp.domain.application.entity.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUser_IdOrderByIdDesc(Long userId);
    List<Application> findByClub_IdOrderByIdDesc(Long clubId);

    boolean existsByUser_IdAndClub_Id(Long userId, Long clubId);
    boolean existsByClub_Id(Long clubId);

    // ✅ 지원수(카운트)
    long countByClub_Id(Long clubId);

    @EntityGraph(attributePaths = {"user"})
    List<Application> findWithUserByClub_IdOrderByIdDesc(Long clubId);

    @EntityGraph(attributePaths = {"user", "club"})
    Optional<Application> findWithUserAndClubById(Long applicationId);
}
