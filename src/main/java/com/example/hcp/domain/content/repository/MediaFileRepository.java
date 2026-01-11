package com.example.hcp.domain.content.repository;

import com.example.hcp.domain.content.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    List<MediaFile> findByClub_IdAndPost_IdOrderByIdAsc(Long clubId, Long postId);
    List<MediaFile> findByClub_IdAndPostIsNullOrderByIdAsc(Long clubId);
}