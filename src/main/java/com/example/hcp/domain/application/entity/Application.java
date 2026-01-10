package com.example.hcp.domain.application.entity;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.account.entity.User;
import com.example.hcp.domain.form.entity.ApplicationForm;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "applications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_club", columnNames = {"user_id", "club_id"})
})
public class Application {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "form_id", nullable = false)
    private ApplicationForm form;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.RECEIVED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Application() {}

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ApplicationStatus.RECEIVED;
    }

}
