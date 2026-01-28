// src/main/java/com/example/hcp/domain/form/entity/ApplicationForm.java
package com.example.hcp.domain.form.entity;

import com.example.hcp.domain.club.entity.Club;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "application_forms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_forms_club", columnNames = "club_id")
})
public class ApplicationForm {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // ✅ 폼에 포함된 "질문 블록" 개수(요청의 totalItems)
    @Setter
    @Column(name = "item_count", nullable = false)
    private int itemCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ApplicationForm() {}

    @PrePersist
    void prePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
