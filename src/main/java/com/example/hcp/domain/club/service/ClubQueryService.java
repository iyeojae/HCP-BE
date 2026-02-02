// src/main/java/com/example/hcp/domain/club/service/ClubQueryService.java
package com.example.hcp.domain.club.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ClubQueryService {

    private final ClubRepository clubRepository;

    public ClubQueryService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    public List<Club> searchPublic(String q, String status) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return clubRepository.searchPublic(q, normalizeStatus(status), now);
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim();
        if (s.isEmpty()) return null;

        return switch (s) {
            case "모집전" -> "PRE";
            case "모집중" -> "OPEN";
            case "모집완료" -> "CLOSED";
            default -> s;
        };
    }

    @Transactional
    public Club getPublicDetailAndIncreaseView(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        club.increaseViewCount();
        return club;
    }
}
