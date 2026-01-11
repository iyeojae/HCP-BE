package com.example.hcp.api.common;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.service.ClubQueryService;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.service.ContentQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/common/clubs")
public class ClubPublicController {

    private final ClubQueryService clubQueryService;
    private final ContentQueryService contentQueryService;

    public ClubPublicController(ClubQueryService clubQueryService, ContentQueryService contentQueryService) {
        this.clubQueryService = clubQueryService;
        this.contentQueryService = contentQueryService;
    }

    @GetMapping
    public List<ClubListResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        List<Club> clubs = clubQueryService.searchPublic(q, status, category);
        return clubs.stream()
                .map(c -> new ClubListResponse(c.getId(), c.getName(), c.getCategory(), c.getRecruitmentStatus()))
                .toList();
    }

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
