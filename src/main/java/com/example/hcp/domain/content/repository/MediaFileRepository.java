// src/main/java/com/example/hcp/domain/content/repository/MediaFileRepository.java
package com.example.hcp.domain.content.repository;

import com.example.hcp.domain.content.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByClub_IdAndPost_IdOrderByIdAsc(Long clubId, Long postId);
    List<MediaFile> findByClub_IdAndPostIsNullOrderByIdAsc(Long clubId);

    boolean existsByClub_IdAndPostIsNullAndIsMainTrue(Long clubId);

    Optional<MediaFile> findTop1ByClub_IdAndPostIsNullAndIsMainTrueAndTypeIgnoreCaseOrderByIdAsc(Long clubId, String type);

    void deleteByClub_IdAndPostIsNull(Long clubId);

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

    @Query("""
        select m from MediaFile m
        where m.club.id in :clubIds
          and m.post is null
          and m.isMain = true
          and lower(m.type) = lower(:type)
        order by m.club.id asc, m.id asc
    """)
    List<MediaFile> findMainByClubIdsAndPostIsNullAndTypeOrderByClubAndId(
            @Param("clubIds") List<Long> clubIds,
            @Param("type") String type
    );

    @Query("""
        select m from MediaFile m
        where m.club.id = :clubId
          and m.post is not null
          and m.post.id in :postIds
        order by m.post.id asc, m.id asc
    """)
    List<MediaFile> findByClubIdAndPostIdsOrderByPostAndId(
            @Param("clubId") Long clubId,
            @Param("postIds") List<Long> postIds
    );
}
