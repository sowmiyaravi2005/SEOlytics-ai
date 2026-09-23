package com.seolytics.service;

import com.seolytics.dto.AuthDtos;
import com.seolytics.exception.ApiException;
import com.seolytics.repository.UserAccountRepository;
import com.seolytics.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void registerHashesPasswordAndIssuesJwt() {
        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest();
        request.setFullName("Ada Lovelace");
        request.setEmail("ada+" + System.nanoTime() + "@example.com");
        request.setPassword("supersecret");
        AuthDtos.AuthResponse response = authService.register(request);
        assertNotNull(response.token());
        assertNotNull(jwtService.parseUserId(response.token()));
        assertTrue(userAccountRepository.findByEmailIgnoreCase(request.getEmail()).isPresent());
        assertTrue(userAccountRepository.findByEmailIgnoreCase(request.getEmail()).get().getPasswordHash().startsWith("$2"));
    }

    @Test
    void duplicateEmailIsRejected() {
        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest();
        request.setFullName("Grace Hopper");
        request.setEmail("grace+" + System.nanoTime() + "@example.com");
        request.setPassword("supersecret");
        authService.register(request);
        assertThrows(ApiException.class, () -> authService.register(request));
    }
}
