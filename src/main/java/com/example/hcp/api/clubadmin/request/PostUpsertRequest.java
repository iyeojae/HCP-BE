package com.example.hcp.api.clubadmin.request;

import jakarta.validation.constraints.NotBlank;

public record PostUpsertRequest(
        @NotBlank String title,
        String content
) {}
