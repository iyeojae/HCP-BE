package com.example.hcp.domain.account.entity;

import com.example.hcp.domain.club.entity.Club;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_admin_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cam_user_club", columnNames = {"user_id", "club_id"})
})
public class ClubAdminMembership {

    @Getter
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Setter
    @Getter
    @Column(name = "is_owner", nullable = false)
    private boolean owner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ClubAdminMembership() {}

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
