package com.example.hcp.domain.account.service;

import com.example.hcp.api.auth.TokenResponse;
import com.example.hcp.domain.account.entity.User;
import com.example.hcp.domain.account.repository.UserRepository;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResult signup(String studentNo, String password) {
        userRepository.findByStudentNo(studentNo).ifPresent(u -> {
            throw new ApiException(ErrorCode.CONFLICT, "STUDENT_NO_ALREADY_EXISTS");
        });

        User user = new User();
        user.setStudentNo(studentNo);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        TokenResponse body = new TokenResponse(accessToken, user.getId(), user.getRole().name(), user.getStudentNo());
        return new AuthResult(body, refreshToken);
    }

    public AuthResult login(String studentNo, String password) {
        User user = userRepository.findByStudentNo(studentNo)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        TokenResponse body = new TokenResponse(accessToken, user.getId(), user.getRole().name(), user.getStudentNo());
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

        TokenResponse body = new TokenResponse(newAccess, user.getId(), user.getRole().name(), user.getStudentNo());
        return new AuthResult(body, newRefresh);
    }
}
