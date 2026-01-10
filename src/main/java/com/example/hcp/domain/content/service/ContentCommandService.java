package com.example.hcp.domain.content.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.domain.content.entity.ClubPost;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.repository.ClubPostRepository;
import com.example.hcp.domain.content.repository.MediaFileRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.example.hcp.infra.FileStorageClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContentCommandService {

    private final ClubRepository clubRepository;
    private final ClubPostRepository postRepository;
    private final MediaFileRepository mediaFileRepository;
    private final FileStorageClient fileStorageClient;

    public ContentCommandService(
            ClubRepository clubRepository,
            ClubPostRepository postRepository,
            MediaFileRepository mediaFileRepository,
            FileStorageClient fileStorageClient
    ) {
        this.clubRepository = clubRepository;
        this.postRepository = postRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.fileStorageClient = fileStorageClient;
    }

    @Transactional
    public ClubPost createPost(Long clubId, String title, String content) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        ClubPost post = new ClubPost();
        post.setClub(club);
        post.setTitle(title);
        post.setContent(content);

        return postRepository.save(post);
    }

    @Transactional
    public MediaFile uploadMedia(Long clubId, Long postIdOrNull, MultipartFile file) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        ClubPost post = null;
        if (postIdOrNull != null) {
            post = postRepository.findById(postIdOrNull)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "POST_NOT_FOUND"));
        }

        FileStorageClient.StoredFile stored = fileStorageClient.store(file);
        String type = (stored.mimeType() != null && stored.mimeType().startsWith("video")) ? "VIDEO" : "IMAGE";

        MediaFile media = new MediaFile();
        media.setClub(club);
        media.setPost(post);
        media.setType(type);
        media.setUrl(stored.url());
        media.setMimeType(stored.mimeType());
        media.setSizeBytes(stored.size());

        return mediaFileRepository.save(media);
    }
}
