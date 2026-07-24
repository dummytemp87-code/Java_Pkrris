package com.example.aichat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory, per-IP sliding-window rate limiter for the handful of
 * endpoints that are reachable without authentication: login/register (brute-force
 * risk) and the AI chat endpoint (unauthenticated, unlimited access to a metered
 * paid API — a direct cost exposure). Appropriate for a single-instance deployment;
 * state resets on restart, which is an acceptable tradeoff at this scale.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int AUTH_LIMIT = 5;
    private static final int CHAT_LIMIT = 20;

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Integer limit = limitFor(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = limit + ":" + clientIp(request);
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());

        boolean allowed;
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            allowed = timestamps.size() < limit;
            if (allowed) {
                timestamps.addLast(now);
            }
        }

        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many attempts, please try again later\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Integer limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)) return null;
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) return AUTH_LIMIT;
        if (path.equals("/api/ai-chat")) return CHAT_LIMIT;
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
