package com.example.hcp.api.admin;

import com.example.hcp.domain.stats.service.AdminDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardAdminController {

    private final AdminDashboardService adminDashboardService;

    public DashboardAdminController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        AdminDashboardService.Result r = adminDashboardService.dashboard();
        return new AdminDashboardResponse(r.totalUsers(), r.totalClubs(), r.totalApplications());
    }
}
