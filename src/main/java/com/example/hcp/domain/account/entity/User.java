package com.example.hcp.domain.account.entity;

import com.example.hcp.global.security.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"),
        @UniqueConstraint(name = "uk_users_student_no", columnNames = "student_no"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
public class User {

    @Getter
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter @Getter
    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Setter @Getter
    @Column(name = "student_no", nullable = false, length = 20)
    private String studentNo;

    @Setter @Getter
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Setter @Getter
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Setter @Getter
    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Setter @Getter
    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Setter @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() {}

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (role == null) role = Role.USER;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
