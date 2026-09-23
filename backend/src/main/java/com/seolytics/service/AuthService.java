package com.seolytics.service;

import com.seolytics.dto.AuthDtos;
import com.seolytics.entity.UserAccount;
import com.seolytics.exception.ApiException;
import com.seolytics.repository.UserAccountRepository;
import com.seolytics.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userAccountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "An account with this email already exists");
        }
        UserAccount user = new UserAccount();
        user.setFullName(request.getFullName().strip());
        user.setEmail(request.getEmail().strip().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userAccountRepository.save(user);
        return toAuth(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.getEmail().strip())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password");
        }
        return toAuth(user);
    }

    public AuthDtos.UserResponse toUser(UserAccount user) {
        return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }

    private AuthDtos.AuthResponse toAuth(UserAccount user) {
        return new AuthDtos.AuthResponse(jwtService.generateToken(user.getId(), user.getEmail()), toUser(user));
    }
}
