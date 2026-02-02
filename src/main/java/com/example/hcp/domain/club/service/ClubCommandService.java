// src/main/java/com/example/hcp/domain/club/service/ClubCommandService.java
package com.example.hcp.domain.club.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClubCommandService {

    private final ClubRepository clubRepository;

    public ClubCommandService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    @Transactional
    public Club create(Club club) {
        validateRecruitPeriod(club.getRecruitStartAt(), club.getRecruitEndAt());
        return clubRepository.save(club);
    }

    @Transactional
    public Club update(Long clubId, Club changes) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        if (changes.getName() != null) club.setName(changes.getName());
        if (changes.getSummary() != null) club.setSummary(changes.getSummary());

        if (changes.getRecruitStartAt() != null) club.setRecruitStartAt(changes.getRecruitStartAt());
        if (changes.getRecruitEndAt() != null) club.setRecruitEndAt(changes.getRecruitEndAt());

        validateRecruitPeriod(club.getRecruitStartAt(), club.getRecruitEndAt());

        if (changes.getCategory() != null) club.setCategory(changes.getCategory());
        if (changes.getIntroduction() != null) club.setIntroduction(changes.getIntroduction());
        if (changes.getInterviewProcess() != null) club.setInterviewProcess(changes.getInterviewProcess());

        return club;
    }

    private void validateRecruitPeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return;
        if (!end.isAfter(start)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "RECRUIT_END_MUST_BE_AFTER_START");
        }
    }
}
