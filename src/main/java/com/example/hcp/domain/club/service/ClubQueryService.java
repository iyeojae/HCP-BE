package com.example.hcp.domain.club.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.entity.ClubCategory;
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

    public List<Club> searchPublic(String q, String status, ClubCategory category) {
        return clubRepository.searchPublic(q, status, category);
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
