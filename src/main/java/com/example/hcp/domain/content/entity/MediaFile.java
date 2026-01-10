package com.example.hcp.domain.content.entity;

import com.example.hcp.domain.club.entity.Club;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
public class MediaFile {

    @Getter
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id")
    private ClubPost post;

    @Setter
    @Getter
    @Column(name = "type", nullable = false, length = 10)
    private String type; // IMAGE/VIDEO

    @Setter
    @Getter
    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Setter
    @Getter
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Setter
    @Getter
    @Column(name = "size_bytes")
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MediaFile() {}

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

}
