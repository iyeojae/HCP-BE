package com.example.hcp.api.common;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.service.ClubQueryService;
import com.example.hcp.domain.content.entity.ClubPost;
import com.example.hcp.domain.content.entity.MediaFile;
import com.example.hcp.domain.content.service.ContentQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

        List<ClubPost> posts = contentQueryService.posts(clubId);
        List<ClubDetailResponse.Post> postDtos = new ArrayList<>();

        for (ClubPost p : posts) {
            List<MediaFile> media = contentQueryService.mediaByPost(clubId, p.getId());
            List<ClubDetailResponse.Media> mediaDtos = media.stream()
                    .map(m -> new ClubDetailResponse.Media(m.getId(), m.getType(), m.getUrl()))
                    .toList();

            postDtos.add(new ClubDetailResponse.Post(p.getId(), p.getTitle(), p.getContent(), mediaDtos));
        }

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
                postDtos
        );
    }
}
