package com.fraudengine.api;

import com.fraudengine.domain.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards admin endpoints with a shared-secret token supplied in the
 * {@code X-Admin-Token} header. Constant-time comparison avoids leaking the
 * token via timing.
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    static final String TOKEN_HEADER = "X-Admin-Token";

    private final String expectedToken;

    public AdminAuthInterceptor(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String provided = request.getHeader(TOKEN_HEADER);
        if (provided == null || !constantTimeEquals(provided, expectedToken)) {
            throw new UnauthorizedException("Missing or invalid admin token");
        }
        return true;
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
