// src/main/java/com/example/hcp/api/admin/AdminClubController.java
package com.example.hcp.api.admin;

import com.example.hcp.domain.club.entity.Club;
import com.example.hcp.domain.club.service.ClubCommandService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClubController {

    private final ClubCommandService clubCommandService;

    public AdminClubController(ClubCommandService clubCommandService) {
        this.clubCommandService = clubCommandService;
    }

    // ✅ 관리자: 동아리 생성(동아리 필드만)
    @PostMapping("/clubs")
    public AdminCreateClubResponse createClub(@Valid @RequestBody AdminCreateClubRequest req) {
        Club c = new Club();
        c.setName(req.name());
        c.setIntroduction(req.introduction());
        c.setActivities(req.activities());
        c.setRecruitTarget(req.recruitTarget());
        c.setInterviewProcess(req.interviewProcess());
        c.setContactLink(req.contactLink());
        c.setCategory(req.category());
        c.setRecruitmentStatus(req.recruitmentStatus());
        c.setPublic(req.isPublic());

        Club saved = clubCommandService.create(c);
        return new AdminCreateClubResponse(saved.getId());
    }
}
