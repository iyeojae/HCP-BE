package com.example.hcp.domain.stats.service;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.repository.ClubRepository;
import com.example.hcp.domain.stats.repository.ClubStatsRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClubDashboardService {

    private final ClubRepository clubRepository;
    private final ClubStatsRepository clubStatsRepository;

    public ClubDashboardService(ClubRepository clubRepository, ClubStatsRepository clubStatsRepository) {
        this.clubRepository = clubRepository;
        this.clubStatsRepository = clubStatsRepository;
    }

    public DashboardResult dashboard(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "CLUB_NOT_FOUND"));

        long totalApplications = clubStatsRepository.totalApplications(clubId);
        long viewCount = club.getViewCount();

        List<Object[]> raw = clubStatsRepository.dailyApplications(clubId);
        List<DailyCount> daily = new ArrayList<>();
        for (Object[] row : raw) {
            LocalDate d = (LocalDate) row[0];
            long c = (long) row[1];
            daily.add(new DailyCount(d.toString(), c));
        }

        return new DashboardResult(totalApplications, viewCount, daily);
    }

    public record DashboardResult(long totalApplications, long viewCount, List<DailyCount> dailyApplications) {}
    public record DailyCount(String date, long count) {}
}
