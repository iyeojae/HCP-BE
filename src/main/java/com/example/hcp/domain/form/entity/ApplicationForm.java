// src/main/java/com/example/hcp/domain/form/entity/ApplicationForm.java
package com.example.hcp.domain.form.entity;

import com.example.hcp.domain.club.entity.Club;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "application_forms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_forms_club", columnNames = "club_id")
})
public class ApplicationForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @Column(name = "item_count", nullable = false)
    private int itemCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ApplicationForm() {}

    public static ApplicationForm create(Club club) {
        if (club == null) throw new IllegalArgumentException("club must not be null");
        ApplicationForm f = new ApplicationForm();
        f.club = club;
        f.itemCount = 0;
        return f;
    }

    public void setItemCount(int itemCount) {
        if (itemCount < 0) throw new IllegalArgumentException("itemCount must be >= 0");
        this.itemCount = itemCount;
    }

    @PrePersist
    void prePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
