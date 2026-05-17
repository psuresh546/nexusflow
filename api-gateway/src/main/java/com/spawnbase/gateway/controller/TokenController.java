package com.spawnbase.gateway.controller;

import com.spawnbase.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Token generation endpoint — development only.
 *
 * In production, tokens are issued by a dedicated
 * auth service (OAuth2, Auth0, Azure AD, etc.)
 *
 * This endpoint lets you generate test tokens to
 * verify RBAC is working correctly.
 *
 * POST /api/auth/token
 * { "userId": "admin-user", "role": "ADMIN" }
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration.ms}")
    private long expirationMs;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(
            @RequestBody Map<String, String> request) {

        String userId = request.getOrDefault(
                "userId", "user-001");
        String role = request.getOrDefault(
                "role", "VIEWER");

        List<String> roles = List.of(role);

        String token = jwtUtil.generateToken(
                userId, roles, expirationMs);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", userId,
                "role", role,
                "type", "Bearer",
                "expiresIn", expirationMs + "ms"
        ));
    }
}