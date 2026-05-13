package com.spawnbase.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — STUB
 *
 * Currently passes all requests through.
 * Day 20 will add full JWT signature validation
 * and RBAC enforcement.
 *
 * When fully implemented:
 * - Missing token → 401 Unauthorized
 * - Invalid signature → 401 Unauthorized
 * - Valid token, wrong role → 403 Forbidden
 * - Valid token, correct role → pass through
 */
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // STUB — log but allow everything through
        // Day 20 replaces this with real JWT validation
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token — " +
                    "passing through (stub mode)");
        } else {
            log.debug("Bearer token present — " +
                    "skipping validation (stub mode)");
        }

        filterChain.doFilter(request, response);
    }
}