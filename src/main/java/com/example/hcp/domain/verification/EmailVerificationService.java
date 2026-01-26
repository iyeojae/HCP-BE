package com.example.hcp.domain.verification;

import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {

    private final EmailVerificationRepository repo;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.school-email-domain:@office.hanseo.ac.kr}")
    private String allowedDomain;

    @Value("${app.email.verification-ttl-seconds:600}")
    private long ttlSeconds;

    public EmailVerificationService(EmailVerificationRepository repo, JavaMailSender mailSender) {
        this.repo = repo;
        this.mailSender = mailSender;
    }

    public void sendCode(String email, EmailPurpose purpose) {
        String normalized = normalizeEmail(email);
        assertSchoolEmail(normalized);

        String code = generate6DigitCode();
        LocalDateTime now = LocalDateTime.now();

        EmailVerification ev = new EmailVerification();
        ev.setEmail(normalized);
        ev.setPurpose(purpose);
        ev.setCode(code);
        ev.setExpiresAt(now.plusSeconds(ttlSeconds));
        ev.setVerifiedAt(null);
        repo.save(ev);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(normalized);
        msg.setSubject("[HCP] 이메일 인증번호");
        msg.setText("인증번호는 " + code + " 입니다.\n유효시간은 " + (ttlSeconds / 60) + "분 입니다.");
        mailSender.send(msg);
    }

    // 코드 검증(성공 시 verifiedAt 기록, 1회용)
    public void verify(String email, EmailPurpose purpose, String code) {
        String normalized = normalizeEmail(email);
        assertSchoolEmail(normalized);

        EmailVerification ev = repo.findTopByEmailAndPurposeOrderByIdDesc(normalized, purpose)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "EMAIL_CODE_NOT_FOUND"));

        LocalDateTime now = LocalDateTime.now();

        if (ev.getVerifiedAt() != null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMAIL_CODE_ALREADY_USED");
        }
        if (ev.getExpiresAt() == null || ev.getExpiresAt().isBefore(now)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMAIL_CODE_EXPIRED");
        }
        if (code == null || code.isBlank() || !code.equals(ev.getCode())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMAIL_CODE_MISMATCH");
        }

        ev.setVerifiedAt(now);
        repo.save(ev);
    }

    private String generate6DigitCode() {
        int n = random.nextInt(900000) + 100000;
        return String.valueOf(n);
    }

    private void assertSchoolEmail(String email) {
        String suffix = (allowedDomain != null && allowedDomain.startsWith("@"))
                ? allowedDomain.toLowerCase()
                : ("@" + String.valueOf(allowedDomain).toLowerCase());

        if (email == null || !email.toLowerCase().endsWith(suffix)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_SCHOOL_EMAIL");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }
}
