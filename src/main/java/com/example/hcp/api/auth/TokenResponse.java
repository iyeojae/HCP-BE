package com.example.hcp.api.auth;

public record TokenResponse(
        String accessToken,
        Long userId,
        String role,
        String studentNo
) {}
