// src/main/java/com/example/hcp/api/common/ClubPublicController.java
package com.example.hcp.api.common;

import com.example.hcp.domain.application.repository.ApplicationRepository;
import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.entity.ClubCategory;
import com.example.hcp.domain.club.service.ClubQueryService;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.service.ContentQueryService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/common/clubs")
public class ClubPublicController {

    private final ClubQueryService clubQueryService;
    private final ContentQueryService contentQueryService;
    private final ApplicationRepository applicationRepository;

    public ClubPublicController(
            ClubQueryService clubQueryService,
            ContentQueryService contentQueryService,
            ApplicationRepository applicationRepository
    ) {
        this.clubQueryService = clubQueryService;
        this.contentQueryService = contentQueryService;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    public List<CategoryGroupResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        List<Club> clubs = clubQueryService.searchPublic(q, status);

        List<Long> clubIds = clubs.stream().map(Club::getId).toList();
        Map<Long, String> coverMap = contentQueryService.clubCoverImageUrlMap(clubIds);

        List<ClubCategory> order = List.of(
                ClubCategory.PERFORMANCE,
                ClubCategory.SPORTS,
                ClubCategory.ACADEMIC,
                ClubCategory.VOLUNTEER,
                ClubCategory.ART,
                ClubCategory.HOBBY,
                ClubCategory.RELIGION
        );

        Map<ClubCategory, List<ClubListResponse>> grouped = new LinkedHashMap<>();
        for (ClubCategory c : order) grouped.put(c, new ArrayList<>());

        for (Club club : clubs) {
            ClubCategory groupKey =
                    (club.getCategory() == null) ? ClubCategory.HOBBY : club.getCategory().displayGroup();
            if (!grouped.containsKey(groupKey)) groupKey = ClubCategory.HOBBY;

            grouped.get(groupKey).add(new ClubListResponse(
                    club.getId(),
                    club.getName(),
                    coverMap.get(club.getId()),
                    oneLine(club.getSummary())
            ));
        }

        return grouped.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> new CategoryGroupResponse(e.getKey(), e.getKey().label(), e.getValue()))
                .toList();
    }

    private String oneLine(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s+", " ").trim();
    }

    public record CategoryGroupResponse(
            ClubCategory category,
            String categoryName,
            List<ClubListResponse> clubs
    ) {}

    @GetMapping("/{clubId}")
    public ClubDetailResponse detail(@PathVariable Long clubId) {
        Club club = clubQueryService.getPublicDetailAndIncreaseView(clubId);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        String recruitState = club.recruitState(now);
        Long secondsToStart = null;
        Long secondsToEnd = null;

        if ("PRE".equals(recruitState) && club.getRecruitStartAt() != null) {
            secondsToStart = Math.max(0, Duration.between(now, club.getRecruitStartAt()).getSeconds());
        }
        if ("OPEN".equals(recruitState) && club.getRecruitEndAt() != null) {
            secondsToEnd = Math.max(0, Duration.between(now, club.getRecruitEndAt()).getSeconds());
        }

        long applicationCount = applicationRepository.countByClub_Id(clubId);

        List<MediaFile> clubMedia = contentQueryService.mediaByClub(clubId);
        List<ClubDetailResponse.Media> clubMediaDtos = clubMedia.stream()
                .map(m -> new ClubDetailResponse.Media(m.getId(), m.getType(), m.getUrl()))
                .toList();

        String mainImageUrl = contentQueryService.clubMainImageUrl(clubId);

        return new ClubDetailResponse(
                club.getId(),
                mainImageUrl,
                club.getName(),
                oneLine(club.getSummary()),
                club.getRecruitStartAt(),
                club.getRecruitEndAt(),
                recruitState,
                secondsToStart,
                secondsToEnd,
                applicationCount,
                club.getCategory(),
                club.getIntroduction(),
                club.getInterviewProcess(),
                club.getViewCount(),
                clubMediaDtos
        );
    }
}
