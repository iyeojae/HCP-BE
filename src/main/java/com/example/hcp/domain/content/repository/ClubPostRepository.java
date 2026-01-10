package com.example.hcp.domain.content.repository;

import com.example.hcp.domain.content.entity.ClubPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubPostRepository extends JpaRepository<ClubPost, Long> {
    List<ClubPost> findByClub_IdOrderByIdDesc(Long clubId);
}
