package com.example.hcp.api.auth;

public record TokenResponse(
        String accessToken,
        Long userId,
        String role,
        String loginId,
        String studentNo,
        String name,
        String department,
        String email
) {}
