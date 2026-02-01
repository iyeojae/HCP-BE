// src/main/java/com/example/hcp/domain/club/service/ClubQueryService.java
package com.example.hcp.domain.club.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClubQueryService {

    private final ClubRepository clubRepository;

    public ClubQueryService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    // q(이름) + status(모집전/모집중/모집완료) 교집합 검색
    public List<Club> searchPublic(String q, String status) {
        return clubRepository.searchPublic(q, normalizeStatus(status));
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim();
        if (s.isEmpty()) return null;

        // 한글/코드 둘 다 허용
        return switch (s) {
            case "모집전" -> "PRE";
            case "모집중" -> "OPEN";
            case "모집완료" -> "CLOSED";
            default -> s; // PRE/OPEN/CLOSED 등
        };
    }

    @Transactional
    public Club getPublicDetailAndIncreaseView(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (!club.isPublic()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND");
        }

        club.increaseViewCount();
        return club;
    }
}
