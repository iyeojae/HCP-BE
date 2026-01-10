package com.example.hcp.domain.stats.service;

import com.example.hcp.domain.stats.repository.AdminStatsRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final AdminStatsRepository adminStatsRepository;

    public AdminDashboardService(AdminStatsRepository adminStatsRepository) {
        this.adminStatsRepository = adminStatsRepository;
    }

    public Result dashboard() {
        return new Result(
                adminStatsRepository.totalUsers(),
                adminStatsRepository.totalClubs(),
                adminStatsRepository.totalApplications()
        );
    }

    public record Result(long totalUsers, long totalClubs, long totalApplications) {}
}
