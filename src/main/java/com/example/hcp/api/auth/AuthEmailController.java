package com.example.hcp.api.auth;

import com.example.hcp.domain.verification.EmailPurpose;
import com.example.hcp.domain.verification.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email")
public class AuthEmailController {

    private final EmailVerificationService emailVerificationService;

    public AuthEmailController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    public record SendEmailCodeRequest(
            @Email @NotBlank String email,
            @NotBlank EmailPurpose purpose
    ) {}

    @PostMapping("/send-code")
    public void sendCode(@Valid @RequestBody SendEmailCodeRequest req) {
        emailVerificationService.sendCode(req.email(), req.purpose());
    }
}
