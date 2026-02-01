package com.example.hcp.domain.verification;

import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {

    private final EmailVerificationRepository repo;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.school-email-domain:@office.hanseo.ac.kr}")
    private String allowedDomain;

    @Value("${app.email.verification-ttl-seconds:600}")
    private long ttlSeconds;

    // ✅ 메일 배경 이미지 URL (정적 리소스로 공개된 주소)
    @Value("${app.mail.bg-url:http://localhost:8080/mail/email-bg.png}")
    private String bgUrl;

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

        long ttlMinutes = Math.max(1, ttlSeconds / 60);
        String purposeLabel = purposeLabel(purpose);

        String subject = "[HCP] " + purposeLabel + " 이메일 인증번호";
        String plainText =
                "인증번호: " + code + "\n" +
                        "유효시간: " + ttlMinutes + "분\n\n" +
                        "본 메일은 자동 발송되었습니다.";

        String html = buildHtml(purposeLabel, code, ttlMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom);
            helper.setTo(normalized);
            helper.setSubject(subject);

            // (plain, html)
            helper.setText(plainText, html);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    private String buildHtml(String purposeLabel, String code, long ttlMinutes) {
        String bg = (bgUrl == null) ? "" : bgUrl.trim();
        // 일부 메일 클라이언트 호환 위해 background 속성 + style 동시 사용
        String bgAttr = bg.isBlank() ? "" : " background='" + escapeAttr(bg) + "'";
        String bgStyle = bg.isBlank()
                ? "background:#080b18;"
                : "background:#080b18 url('" + escapeCssUrl(bg) + "') no-repeat center/cover;";

        return ""
                + "<!doctype html>"
                + "<html lang='ko'>"
                + "<head>"
                + "  <meta charset='UTF-8' />"
                + "  <meta name='viewport' content='width=device-width, initial-scale=1.0' />"
                + "  <title>HCP 이메일 인증</title>"
                + "</head>"
                + "<body style='margin:0;padding:0;background:#080b18;'>"

                + "  <table role='presentation' width='100%' cellspacing='0' cellpadding='0'"
                + "         style='width:100%;" + bgStyle + "background-repeat:no-repeat;background-size:cover;'" + bgAttr + ">"
                + "    <tr>"
                + "      <td align='center' style='padding:52px 16px;'>"

                // 중앙 카드
                + "        <table role='presentation' width='680' cellspacing='0' cellpadding='0'"
                + "               style='max-width:680px;width:100%;border-radius:22px;overflow:hidden;"
                + "                      border:1px solid rgba(255,255,255,0.14);"
                + "                      background:rgba(12,14,26,0.72);"
                + "                      box-shadow:0 14px 40px rgba(0,0,0,0.45);'>"
                + "          <tr>"
                + "            <td style='padding:30px 32px 18px 32px;'>"
                + "              <div style='font-size:14px;letter-spacing:3px;color:rgba(255,255,255,0.62);'>한서클럽포탈</div>"
                + "              <div style='margin-top:10px;font-size:30px;line-height:1.2;color:#ffffff;font-weight:800;'>"
                +                    escapeHtml(purposeLabel) + " 인증번호"
                + "              </div>"
                + "              <div style='margin-top:10px;font-size:15px;line-height:1.65;color:rgba(255,255,255,0.80);'>"
                + "                아래 인증번호를 입력해 주세요."
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:0 32px 18px 32px;'>"
                + "              <div style='border-radius:16px;"
                + "                          background:rgba(0,0,0,0.35);"
                + "                          border:1px solid rgba(255,255,255,0.16);"
                + "                          padding:22px 16px;text-align:center;'>"
                + "                <div style='margin-top:12px;font-size:42px;letter-spacing:10px;"
                + "                            color:#ffffff;font-weight:900;'>"
                +                    escapeHtml(code)
                + "                </div>"
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:0 32px 26px 32px;'>"
                + "              <div style='font-size:14px;line-height:1.75;color:rgba(255,255,255,0.78);'>"
                + "                유효시간: <b style='color:#ffffff;'>" + ttlMinutes + "분</b><br/>"
                + "              </div>"
                + "              <div style='margin-top:16px;height:1px;background:rgba(255,255,255,0.10);'></div>"
                + "              <div style='margin-top:14px;font-size:12px;line-height:1.6;color:rgba(255,255,255,0.55);'>"
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String purposeLabel(EmailPurpose purpose) {
        if (purpose == null) return "이메일";
        String name = purpose.name();
        return switch (name) {
            case "SIGNUP" -> "회원가입";
            case "FIND_ID" -> "아이디 찾기";
            case "RESETPASSWORD", "RESET_PASSWORD" -> "비밀번호 재설정";
            default -> "이메일";
        };
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

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeAttr(String s) {
        // attribute 값에서 ' 를 최소한으로 방어
        if (s == null) return "";
        return s.replace("'", "%27");
    }

    private String escapeCssUrl(String s) {
        // url('...') 내부에서 ' 와 \ 만 최소 방어
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
