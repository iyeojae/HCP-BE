package com.example.hcp.domain.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClubStatsRepository extends JpaRepository<com.example.hcp.domain.club.entity.Club, Long> {

    @Query("select count(a) from Application a where a.club.id = :clubId")
    long totalApplications(Long clubId);

    @Query("""
        select function('date', a.createdAt), count(a)
        from Application a
        where a.club.id = :clubId
        group by function('date', a.createdAt)
        order by function('date', a.createdAt) asc
    """)
    List<Object[]> dailyApplications(Long clubId);
}
