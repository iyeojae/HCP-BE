// src/main/java/com/example/hcp/domain/content/service/ContentCommandService.java
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
    public void updatePost(Long clubId, Long postId, String title, String content) {
        ClubPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "POST_NOT_FOUND"));

        if (!post.getClub().getId().equals(clubId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "CLUB_ACCESS_DENIED");
        }

        post.setTitle(title);
        post.setContent(content);
    }

    @Transactional
    public MediaFile uploadMedia(Long clubId, Long postIdOrNull, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_FILE");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        ClubPost post = null;
        if (postIdOrNull != null) {
            post = postRepository.findById(postIdOrNull)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "POST_NOT_FOUND"));

            if (!post.getClub().getId().equals(clubId)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "CLUB_ACCESS_DENIED");
            }
        }

        FileStorageClient.StoredFile stored = fileStorageClient.store(file);

        String mime = (stored.mimeType() != null && !stored.mimeType().isBlank())
                ? stored.mimeType()
                : file.getContentType();

        String type = (mime != null && mime.toLowerCase().startsWith("video/"))
                ? "VIDEO"
                : "IMAGE";

        MediaFile media = new MediaFile();
        media.setClub(club);
        media.setPost(post);
        media.setType(type);
        media.setUrl(stored.url());
        media.setMimeType(mime);
        media.setSizeBytes(stored.size());

        // club-level(post=null) + IMAGE: 메인이 없으면 자동 메인 지정
        if (post == null && "IMAGE".equalsIgnoreCase(type)) {
            boolean hasMain = mediaFileRepository.existsByClub_IdAndPostIsNullAndIsMainTrue(clubId);
            media.setMain(!hasMain);
        } else {
            media.setMain(false);
        }

        return mediaFileRepository.save(media);
    }
}
