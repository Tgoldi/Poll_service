package com.example.pollservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Enforces {@link AdminOnly} on the handler Spring MVC has routed the request to.
 * The admin token is read from the POLL_ADMIN_TOKEN environment variable.
 * When it is not set (or too short), every admin action is refused.
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Admin-Token";
    private static final int MIN_TOKEN_LENGTH = 16;
    private static final Logger log = LoggerFactory.getLogger(AdminTokenInterceptor.class);

    private final byte[] adminToken;

    public AdminTokenInterceptor(@Value("${POLL_ADMIN_TOKEN:}") String configuredToken) {
        String token = configuredToken == null ? "" : configuredToken.trim();
        if (token.length() < MIN_TOKEN_LENGTH) {
            log.warn("POLL_ADMIN_TOKEN is not set (or shorter than {} characters): creating and deleting "
                    + "questions and listing, reading and deleting responses are disabled until it is set. Callers send it in the "
                    + "{} header.", MIN_TOKEN_LENGTH, HEADER);
            this.adminToken = null;
        } else {
            this.adminToken = token.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod method = (HandlerMethod) handler;
        boolean adminOnly = method.hasMethodAnnotation(AdminOnly.class)
                || method.getBeanType().isAnnotationPresent(AdminOnly.class);
        if (!adminOnly) {
            return true;
        }
        if (adminToken == null) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        String supplied = request.getHeader(HEADER);
        if (supplied == null || supplied.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        if (!MessageDigest.isEqual(adminToken, supplied.trim().getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        return true;
    }
}
