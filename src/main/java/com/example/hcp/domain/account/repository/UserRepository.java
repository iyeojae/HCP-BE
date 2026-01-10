package com.example.hcp.domain.account.repository;

import com.example.hcp.domain.account.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentNo(String studentNo);
    Optional<User> findByLoginId(String loginId);
}
