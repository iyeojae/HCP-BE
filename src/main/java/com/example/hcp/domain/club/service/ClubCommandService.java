package com.example.hcp.domain.club.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClubCommandService {

    private final ClubRepository clubRepository;

    public ClubCommandService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    @Transactional
    public Club create(Club club) {
        return clubRepository.save(club);
    }

    @Transactional
    public Club update(Long clubId, Club changes) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        club.setName(changes.getName());
        club.setIntroduction(changes.getIntroduction());
        club.setActivities(changes.getActivities());
        club.setRecruitTarget(changes.getRecruitTarget());
        club.setInterviewProcess(changes.getInterviewProcess());
        club.setContactLink(changes.getContactLink());
        club.setCategory(changes.getCategory());
        club.setRecruitmentStatus(changes.getRecruitmentStatus());
        club.setPublic(changes.isPublic());

        return club;
    }
}
