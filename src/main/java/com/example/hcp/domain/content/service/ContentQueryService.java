// src/main/java/com/example/hcp/domain/content/service/ContentQueryService.java
package com.example.hcp.domain.content.service;

import com.example.hcp.domain.content.entity.ClubPost;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.repository.ClubPostRepository;
import com.example.hcp.domain.content.repository.MediaFileRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public List<MediaFile> mediaByClub(Long clubId) {
        return mediaFileRepository.findByClub_IdAndPostIsNullOrderByIdAsc(clubId);
    }

    // clubIds에 대해 대표사진 URL(첫 IMAGE) 맵으로 반환
    public Map<Long, String> clubCoverImageUrlMap(List<Long> clubIds) {
        if (clubIds == null || clubIds.isEmpty()) return Map.of();

        List<MediaFile> images = mediaFileRepository
                .findByClubIdsAndPostIsNullAndTypeOrderByClubAndId(clubIds, "IMAGE");

        Map<Long, String> map = new HashMap<>();
        for (MediaFile m : images) {
            Long clubId = m.getClub().getId();
            map.putIfAbsent(clubId, m.getUrl()); // 첫 이미지가 대표
        }
        return map;
    }
}
