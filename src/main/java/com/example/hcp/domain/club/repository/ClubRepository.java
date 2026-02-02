// src/main/java/com/example/hcp/domain/club/repository/ClubRepository.java
package com.example.hcp.domain.club.repository;

import com.example.hcp.domain.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
        select c from Club c
        where (:q is null or :q = '' or c.name like concat('%', :q, '%'))
          and (
               :status is null or :status = '' or
               (:status = 'PRE'    and :now <  c.recruitStartAt) or
               (:status = 'OPEN'   and :now >= c.recruitStartAt and :now <= c.recruitEndAt) or
               (:status = 'CLOSED' and :now >  c.recruitEndAt)
          )
        order by c.name asc
    """)
    List<Club> searchPublic(
            @Param("q") String q,
            @Param("status") String status,
            @Param("now") LocalDateTime now
    );
}
