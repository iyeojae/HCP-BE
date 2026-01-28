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

    // FormCommandService에서 폼 수정 잠금에 사용
    boolean existsByClub_Id(Long clubId);

    // ✅ 목록에서 user 미리 로딩
    @EntityGraph(attributePaths = {"user"})
    List<Application> findWithUserByClub_IdOrderByIdDesc(Long clubId);

    // ✅ 상세에서 user, club 미리 로딩
    @EntityGraph(attributePaths = {"user", "club"})
    Optional<Application> findWithUserAndClubById(Long applicationId);
}
