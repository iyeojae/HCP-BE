package com.example.hcp.domain.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminStatsRepository extends JpaRepository<com.example.hcp.domain.club.entity.Club, Long> {

    @Query("select count(u) from User u")
    long totalUsers();

    @Query("select count(c) from Club c")
    long totalClubs();

    @Query("select count(a) from Application a")
    long totalApplications();
}
