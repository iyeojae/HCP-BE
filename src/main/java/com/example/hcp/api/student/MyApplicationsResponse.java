package com.example.hcp.api.student;

import java.util.List;

public record MyApplicationsResponse(
        List<Item> items
) {
    public record Item(
            Long applicationId,
            Long clubId,
            String clubName,
            String status,
            String createdAt
    ) {}
}
