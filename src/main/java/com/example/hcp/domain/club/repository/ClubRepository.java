package com.example.hcp.domain.club.repository;

import com.example.hcp.domain.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
        select c from Club c
        where c.isPublic = true
          and (:q is null or :q = '' or c.name like concat('%', :q, '%'))
          and (:status is null or :status = '' or c.recruitmentStatus = :status)
          and (:category is null or :category = '' or c.category = :category)
        order by c.name asc
    """)
    List<Club> searchPublic(String q, String status, String category);
}
