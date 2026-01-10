package com.example.hcp.api.auth;

import com.example.hcp.domain.account.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.security.refresh-cookie-name:REFRESH_TOKEN}")
    private String refreshCookieName;

    @Value("${app.security.refresh-cookie-domain:}")
    private String refreshCookieDomain;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.security.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${app.jwt.refresh-token-ttl-seconds}")
    private long refreshTtlSeconds;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public TokenResponse signup(@Valid @RequestBody SignupRequest req, HttpServletResponse res) {
        AuthService.AuthResult r = authService.signup(
                req.loginId(), req.studentNo(), req.name(), req.department(), req.password()
        );
        setRefreshCookie(res, r.refreshToken());
        return r.response();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        AuthService.AuthResult r = authService.login(req.loginId(), req.password());
        setRefreshCookie(res, r.refreshToken());
        return r.response();
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String refreshToken = readCookie(req, refreshCookieName);
        AuthService.AuthResult r = authService.refresh(refreshToken);
        setRefreshCookie(res, r.refreshToken());
        return r.response();
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse res) {
        clearRefreshCookie(res);
    }

    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse res, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(refreshTtlSeconds)
                .sameSite(refreshCookieSameSite);

        if (refreshCookieDomain != null && !refreshCookieDomain.isBlank()) {
            b.domain(refreshCookieDomain.trim());
        }

        res.addHeader("Set-Cookie", b.build().toString());
    }

    private void clearRefreshCookie(HttpServletResponse res) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(refreshCookieSameSite);

        if (refreshCookieDomain != null && !refreshCookieDomain.isBlank()) {
            b.domain(refreshCookieDomain.trim());
        }

        res.addHeader("Set-Cookie", b.build().toString());
    }
}
