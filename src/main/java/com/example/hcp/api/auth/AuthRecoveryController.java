package com.example.hcp.api.auth;

import com.example.hcp.domain.account.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/recovery")
public class AuthRecoveryController {

    private final AuthService authService;

    public AuthRecoveryController(AuthService authService) {
        this.authService = authService;
    }

    public record FindLoginIdRequest(
            @Email @NotBlank String email,
            @NotBlank String code
    ) {}

    public record FindLoginIdResponse(String loginId) {}

    @PostMapping("/find-id")
    public FindLoginIdResponse findId(@Valid @RequestBody FindLoginIdRequest req) {
        String loginId = authService.findLoginIdByEmail(req.email(), req.code());
        return new FindLoginIdResponse(loginId);
    }

    public record ResetPasswordRequest(
            @Email @NotBlank String email,
            @NotBlank String code,
            @NotBlank String newPassword
    ) {}

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPasswordByEmail(req.email(), req.code(), req.newPassword());
    }
}
