// src/main/java/com/example/hcp/domain/content/repository/MediaFileRepository.java
package com.example.hcp.domain.content.repository;

import com.example.hcp.domain.content.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByClub_IdAndPost_IdOrderByIdAsc(Long clubId, Long postId);
    List<MediaFile> findByClub_IdAndPostIsNullOrderByIdAsc(Long clubId);

    // 여러 동아리 대표사진(IMAGE, post=null) 한 번에 조회
    @Query("""
        select m from MediaFile m
        where m.club.id in :clubIds
          and m.post is null
          and lower(m.type) = lower(:type)
        order by m.club.id asc, m.id asc
    """)
    List<MediaFile> findByClubIdsAndPostIsNullAndTypeOrderByClubAndId(
            @Param("clubIds") List<Long> clubIds,
            @Param("type") String type
    );
}
