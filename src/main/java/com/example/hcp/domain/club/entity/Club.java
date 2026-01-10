package com.example.hcp.domain.club.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clubs")
public class Club {

    @Getter
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Setter
    @Getter
    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Setter
    @Getter
    @Column(name = "activities", columnDefinition = "TEXT")
    private String activities;

    @Setter
    @Getter
    @Column(name = "recruit_target", columnDefinition = "TEXT")
    private String recruitTarget;

    @Setter
    @Getter
    @Column(name = "interview_process", columnDefinition = "TEXT")
    private String interviewProcess;

    @Setter
    @Getter
    @Column(name = "contact_link", length = 500)
    private String contactLink;

    @Setter
    @Getter
    @Column(name = "category", length = 50)
    private String category;

    @Setter
    @Getter
    @Column(name = "recruitment_status", length = 20)
    private String recruitmentStatus; // OPEN/CLOSED/PRE 등

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @Getter
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Club() {}

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPublic() { return isPublic; }

    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
