package com.example.hcp.api.auth;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String studentNo,
        @NotBlank String password
) {}
