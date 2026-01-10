package com.example.hcp.api.clubadmin.response;

import java.util.List;

public record DashboardResponse(
        long totalApplications,
        long viewCount,
        List<Daily> dailyApplications
) {
    public record Daily(String date, long count) {}
}
