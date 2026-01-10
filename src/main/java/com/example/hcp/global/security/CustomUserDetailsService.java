package com.example.hcp.global.security;

import com.example.hcp.domain.account.entity.User;
import com.example.hcp.domain.account.repository.UserRepository;
import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // JWT 필터에서 userId를 username처럼 넘김
    @Override
    public UserDetails loadUserByUsername(String username) {
        Long userId;
        try {
            userId = Long.parseLong(username);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        return new SecurityUser(user.getId(), user.getRole());
    }
}
