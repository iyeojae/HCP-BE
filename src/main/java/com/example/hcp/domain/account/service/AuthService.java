package com.example.hcp.domain.account.service;

import com.example.hcp.api.auth.TokenResponse;
import com.example.hcp.domain.account.entity.User;
import com.example.hcp.domain.account.repository.UserRepository;
import com.example.hcp.domain.verification.EmailPurpose;
import com.example.hcp.domain.verification.EmailVerificationService;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import com.example.hcp.global.security.JwtTokenProvider;
import com.example.hcp.global.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public record AuthResult(TokenResponse response, String refreshToken) {}

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationService = emailVerificationService;
    }

    public AuthResult signup(
            String name, String department, Integer grade, String password,
            String email, String code
    ) {
        // 이메일 인증(회원가입)
        emailVerificationService.verify(email, EmailPurpose.SIGNUP, code);

        // email 앞부분(로컬파트)로 loginId/studentNo 생성
        String derived = deriveIdFromEmail(email);
        String loginId = derived;
        String studentNo = derived;

        userRepository.findByLoginId(loginId).ifPresent(u -> {
            throw new ApiException(ErrorCode.CONFLICT, "LOGIN_ID_ALREADY_EXISTS");
        });

        userRepository.findByStudentNo(studentNo).ifPresent(u -> {
            throw new ApiException(ErrorCode.CONFLICT, "STUDENT_NO_ALREADY_EXISTS");
        });

        userRepository.findByEmail(email).ifPresent(u -> {
            throw new ApiException(ErrorCode.CONFLICT, "EMAIL_ALREADY_EXISTS");
        });

        User user = new User();
        user.setLoginId(loginId);
        user.setStudentNo(studentNo);
        user.setName(name);
        user.setDepartment(department);
        user.setGrade(grade); // ✅ User에 grade 필드/Setter 추가 필요
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        TokenResponse body = new TokenResponse(
                accessToken,
                user.getId(),
                user.getRole().name(),
                user.getLoginId(),
                user.getStudentNo(),
                user.getName(),
                user.getDepartment(),
                user.getGrade(),   // ✅ TokenResponse에 grade 필드 추가 필요
                user.getEmail()
        );
        return new AuthResult(body, refreshToken);
    }

    public AuthResult login(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        TokenResponse body = new TokenResponse(
                accessToken,
                user.getId(),
                user.getRole().name(),
                user.getLoginId(),
                user.getStudentNo(),
                user.getName(),
                user.getDepartment(),
                user.getGrade(),
                user.getEmail()
        );
        return new AuthResult(body, refreshToken);
    }

    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "MISSING_REFRESH_TOKEN");
        }

        Long userId = jwtTokenProvider.validateAndGetUserIdFromRefreshToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        String newAccess = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String newRefresh = jwtTokenProvider.createRefreshToken(user.getId());

        TokenResponse body = new TokenResponse(
                newAccess,
                user.getId(),
                user.getRole().name(),
                user.getLoginId(),
                user.getStudentNo(),
                user.getName(),
                user.getDepartment(),
                user.getGrade(),
                user.getEmail()
        );
        return new AuthResult(body, newRefresh);
    }

    // 아이디 찾기 (이메일 + 코드)
    public String findLoginIdByEmail(String email, String code) {
        emailVerificationService.verify(email, EmailPurpose.FIND_ID, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "USER_NOT_FOUND"));

        return user.getLoginId();
    }

    // 비밀번호 재설정 (이메일 + 코드)
    public void resetPasswordByEmail(String email, String code, String newPassword) {
        emailVerificationService.verify(email, EmailPurpose.RESET_PASSWORD, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "USER_NOT_FOUND"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String deriveIdFromEmail(String email) {
        if (email == null) throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_EMAIL");
        String e = email.trim().toLowerCase();

        int at = e.indexOf('@');
        if (at <= 0) throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_EMAIL");

        String local = e.substring(0, at).trim();
        if (local.isEmpty()) throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_EMAIL");

        // 요구사항: @ 앞 숫자를 사용 → 숫자 아니면 거부
        for (int i = 0; i < local.length(); i++) {
            if (!Character.isDigit(local.charAt(i))) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "INVALID_EMAIL_LOCALPART");
            }
        }
        return local;
    }
}
