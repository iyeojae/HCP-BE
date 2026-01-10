package com.example.hcp.api.clubadmin.request;

import com.example.hcp.domain.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull ApplicationStatus status
) {}
