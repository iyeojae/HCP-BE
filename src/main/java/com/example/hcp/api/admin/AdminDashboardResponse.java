package com.example.hcp.api.admin;

public record AdminDashboardResponse(
        long totalUsers,
        long totalClubs,
        long totalApplications
) {}
