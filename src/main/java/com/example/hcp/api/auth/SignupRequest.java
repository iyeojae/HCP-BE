package com.example.hcp.api.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        @NotBlank String loginId,                 // ✅ 추가
        @NotBlank String name,
        @NotBlank String department,
        @NotNull @Min(1) @Max(6) Integer grade,   // 학년
        @NotBlank String password,
        @NotBlank String code
) {}
