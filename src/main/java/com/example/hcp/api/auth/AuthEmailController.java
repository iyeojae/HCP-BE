package com.example.hcp.api.auth;

import com.example.hcp.domain.verification.EmailPurpose;
import com.example.hcp.domain.verification.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
public class AuthEmailController {

    private final EmailVerificationService emailVerificationService;

    @Value("${app.school-email-domain:@office.hanseo.ac.kr}")
    private String schoolEmailDomain;

    public AuthEmailController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    public record SendEmailCodeRequest(
            @NotBlank String loginId,        // ✅ 학번(=loginId)만 받음
            @NotNull EmailPurpose purpose
    ) {}

    @PostMapping("/send-code")
    public void sendCode(@Valid @RequestBody SendEmailCodeRequest req) {
        String email = buildSchoolEmail(req.loginId());
        emailVerificationService.sendCode(email, req.purpose());
    }

    private String buildSchoolEmail(String loginId) {
        String id = loginId.trim();

        String suffix = (schoolEmailDomain != null && schoolEmailDomain.startsWith("@"))
                ? schoolEmailDomain.trim()
                : ("@" + String.valueOf(schoolEmailDomain).trim());

        return (id + suffix).toLowerCase();
    }
}
