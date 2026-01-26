package com.example.hcp.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String loginId,
        @NotBlank String studentNo,
        @NotBlank String name,
        @NotBlank String department,
        @NotBlank String password,
        @Email @NotBlank String email,
        @NotBlank String code
) {}
