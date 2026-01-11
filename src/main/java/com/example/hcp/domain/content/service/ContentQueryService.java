package com.example.hcp.domain.content.service;

import com.example.hcp.domain.content.entity.ClubPost;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.repository.ClubPostRepository;
import com.example.hcp.domain.content.repository.MediaFileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
