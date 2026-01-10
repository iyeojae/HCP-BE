package com.example.hcp.domain.account.repository;

import com.example.hcp.domain.account.entity.ClubAdminMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClubAdminMembershipRepository extends JpaRepository<ClubAdminMembership, Long> {

    boolean existsByUser_IdAndClub_Id(Long userId, Long clubId);

    @Query("select m.club.id from ClubAdminMembership m where m.user.id = :userId")
    List<Long> findClubIdsByUserId(Long userId);
}
