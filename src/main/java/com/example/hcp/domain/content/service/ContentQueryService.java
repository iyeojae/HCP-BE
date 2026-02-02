// src/main/java/com/example/hcp/domain/content/service/ContentQueryService.java
package com.example.hcp.domain.content.service;

import com.example.hcp.domain.content.entity.ClubPost;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.repository.ClubPostRepository;
import com.example.hcp.domain.content.repository.MediaFileRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContentQueryService {

    private final ClubPostRepository postRepository;
    private final MediaFileRepository mediaFileRepository;

    public ContentQueryService(ClubPostRepository postRepository, MediaFileRepository mediaFileRepository) {
        this.postRepository = postRepository;
        this.mediaFileRepository = mediaFileRepository;
    }

    public List<ClubPost> posts(Long clubId) {
        return postRepository.findByClub_IdOrderByIdDesc(clubId);
    }

    public List<MediaFile> mediaByPost(Long clubId, Long postId) {
        return mediaFileRepository.findByClub_IdAndPost_IdOrderByIdAsc(clubId, postId);
    }

    // ✅ [추가] 여러 post 미디어 일괄 조회 (N+1 제거)
    public Map<Long, List<MediaFile>> mediaByPosts(Long clubId, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of(); // ✅ 빈 IN 방지

        List<MediaFile> list = mediaFileRepository.findByClubIdAndPostIdsOrderByPostAndId(clubId, postIds);

        Map<Long, List<MediaFile>> map = new HashMap<>();
        for (MediaFile m : list) {
            Long postId = (m.getPost() != null) ? m.getPost().getId() : null;
            if (postId == null) continue;
            map.computeIfAbsent(postId, k -> new ArrayList<>()).add(m);
        }
        return map;
    }

    public List<MediaFile> mediaByClub(Long clubId) {
        return mediaFileRepository.findByClub_IdAndPostIsNullOrderByIdAsc(clubId);
    }

    public String clubMainImageUrl(Long clubId) {
        return mediaFileRepository
                .findTop1ByClub_IdAndPostIsNullAndIsMainTrueAndTypeIgnoreCaseOrderByIdAsc(clubId, "IMAGE")
                .map(MediaFile::getUrl)
                .orElseGet(() ->
                        mediaByClub(clubId).stream()
                                .filter(m -> "IMAGE".equalsIgnoreCase(m.getType()))
                                .map(MediaFile::getUrl)
                                .findFirst()
                                .orElse(null)
                );
    }

    public Map<Long, String> clubCoverImageUrlMap(List<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) return Map.of();

        Map<Long, String> map = new HashMap<>();

        List<MediaFile> mains = mediaFileRepository
                .findMainByClubIdsAndPostIsNullAndTypeOrderByClubAndId(clubIds, "IMAGE");
        for (MediaFile m : mains) {
            map.putIfAbsent(m.getClub().getId(), m.getUrl());
        }

        if (map.size() < clubIds.size()) {
            List<MediaFile> images = mediaFileRepository
                    .findByClubIdsAndPostIsNullAndTypeOrderByClubAndId(clubIds, "IMAGE");
            for (MediaFile m : images) {
                map.putIfAbsent(m.getClub().getId(), m.getUrl());
            }
        }

        return map;
    }
}
