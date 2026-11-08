package com.example.pollservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Enforces {@link AdminOnly} and {@link ProtectedRead} on the handler Spring
 * actually routes the request to, so encoded paths or path variants cannot
 * slip past it.
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminTokenInterceptor.class);

    public static final String HEADER = "X-Admin-Token";

    /** SHA-256 of the configured admin token, or null when none is set. */
    private final byte[] adminTokenHash;

    public AdminTokenInterceptor(@Value("${POLL_ADMIN_TOKEN:}") String adminToken) {
        if (adminToken == null || adminToken.isBlank()) {
            this.adminTokenHash = null;
            log.warn("POLL_ADMIN_TOKEN is not set: creating questions, deleting questions and deleting responses "
                    + "are refused (403) until it is set, and GET /responses stays public. Set POLL_ADMIN_TOKEN to a "
                    + "long random value and send it in the " + HEADER + " header.");
        } else {
            this.adminTokenHash = sha256(adminToken.trim());
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        boolean adminOnly = has(handlerMethod, AdminOnly.class);
        boolean protectedRead = has(handlerMethod, ProtectedRead.class);
        if (!adminOnly && !protectedRead) {
            return true;
        }
        if (adminTokenHash == null) {
            if (adminOnly) {
                response.sendError(HttpStatus.FORBIDDEN.value());
                return false;
            }
            // Protected reads stay public until the owner configures the token.
            return true;
        }
        String supplied = request.getHeader(HEADER);
        if (supplied == null || !MessageDigest.isEqual(sha256(supplied.trim()), adminTokenHash)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        return true;
    }

    private static boolean has(HandlerMethod handlerMethod, Class<? extends java.lang.annotation.Annotation> type) {
        return handlerMethod.hasMethodAnnotation(type)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), type);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
