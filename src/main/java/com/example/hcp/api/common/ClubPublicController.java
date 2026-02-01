// src/main/java/com/example/hcp/api/common/ClubPublicController.java
package com.example.hcp.api.common;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.entity.ClubCategory;
import com.example.hcp.domain.club.service.ClubQueryService;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.service.ContentQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/common/clubs")
public class ClubPublicController {

    private final ClubQueryService clubQueryService;
    private final ContentQueryService contentQueryService;

    public ClubPublicController(ClubQueryService clubQueryService, ContentQueryService contentQueryService) {
        this.clubQueryService = clubQueryService;
        this.contentQueryService = contentQueryService;
    }

    // 단일 검색/목록: status만 => 해당 상태 전체, q+status => 교집합
    // 응답은 카테고리별로 그룹핑
    @GetMapping
    public List<CategoryGroupResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        List<Club> clubs = clubQueryService.searchPublic(q, status);

        List<Long> clubIds = clubs.stream().map(Club::getId).toList();
        Map<Long, String> coverMap = contentQueryService.clubCoverImageUrlMap(clubIds);

        // 표시 순서 고정(요구한 7개)
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
            ClubCategory groupKey = club.getCategory().displayGroup();

            // 혹시 DB에 예상 외 값이 들어와도 누락 방지(기본: 취미분야)
            if (!grouped.containsKey(groupKey)) groupKey = ClubCategory.HOBBY;

            grouped.get(groupKey).add(new ClubListResponse(
                    club.getId(),
                    club.getName(),
                    coverMap.get(club.getId()),
                    oneLine(club.getIntroduction())
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
            String categoryName,           // 공연분야/체육분야...
            List<ClubListResponse> clubs   // 각 동아리: 이름/대표사진/한줄소개
    ) {}

    @GetMapping("/{clubId}")
    public ClubDetailResponse detail(@PathVariable Long clubId) {
        Club club = clubQueryService.getPublicDetailAndIncreaseView(clubId);

        List<MediaFile> media = contentQueryService.mediaByClub(clubId);
        List<ClubDetailResponse.Media> mediaDtos = media.stream()
                .map(m -> new ClubDetailResponse.Media(m.getId(), m.getType(), m.getUrl()))
                .toList();

        return new ClubDetailResponse(
                club.getId(),
                club.getName(),
                club.getIntroduction(),
                club.getActivities(),
                club.getRecruitTarget(),
                club.getInterviewProcess(),
                club.getContactLink(),
                club.getCategory(),
                club.getRecruitmentStatus(),
                club.getViewCount(),
                mediaDtos
        );
    }
}
