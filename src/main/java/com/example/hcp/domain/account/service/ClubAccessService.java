package com.example.hcp.domain.account.service;

import com.example.hcp.domain.account.repository.ClubAdminMembershipRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClubAccessService {

    private final ClubAdminMembershipRepository membershipRepository;

    public ClubAccessService(ClubAdminMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public void assertClubAdminAccess(Long userId, Long clubId) {
        boolean ok = membershipRepository.existsByUser_IdAndClub_Id(userId, clubId);
        if (!ok) {
            throw new ApiException(ErrorCode.FORBIDDEN, "CLUB_ACCESS_DENIED");
        }
    }

    public List<Long> myClubIds(Long userId) {
        return membershipRepository.findClubIdsByUserId(userId);
    }
}
