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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Ties a respondent action to a real user.
 *
 * The system that knows who the user is (the site or app that logs users in) signs
 * the user id with the shared key POLL_USER_SIGNING_KEY and sends it in the
 * X-User-Token header as:
 *
 *   userId.expiresAtEpochSeconds.signature
 *
 * where signature is the lowercase hex HMAC-SHA256 of "userId.expiresAtEpochSeconds"
 * under POLL_USER_SIGNING_KEY. The verified user id replaces whatever user id the
 * client put in the request body.
 *
 * When POLL_USER_SIGNING_KEY is not set (or too short), the check is off and the
 * app behaves as before; a warning is logged at start.
 */
@Component
public class RespondentTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-User-Token";
    public static final String VERIFIED_USER_ID = "com.example.pollservice.security.verifiedUserId";
    private static final int MIN_KEY_LENGTH = 32;
    private static final int MAX_TOKEN_LENGTH = 256;
    private static final Logger log = LoggerFactory.getLogger(RespondentTokenInterceptor.class);

    private final byte[] signingKey;

    public RespondentTokenInterceptor(@Value("${POLL_USER_SIGNING_KEY:}") String configuredKey) {
        String key = configuredKey == null ? "" : configuredKey.trim();
        if (key.length() < MIN_KEY_LENGTH) {
            log.warn("POLL_USER_SIGNING_KEY is not set (or shorter than {} characters): POST /responses accepts the "
                    + "userId sent by the client, so anyone can answer in another user's name. Set it and have your "
                    + "login system send a signed {} header.", MIN_KEY_LENGTH, HEADER);
            this.signingKey = null;
        } else {
            this.signingKey = key.getBytes(StandardCharsets.UTF_8);
        }
    }

    public boolean isEnabled() {
        return signingKey != null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod method = (HandlerMethod) handler;
        if (!method.hasMethodAnnotation(RespondentIdentity.class) || signingKey == null) {
            return true;
        }
        Long userId = verify(request.getHeader(HEADER));
        if (userId == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        request.setAttribute(VERIFIED_USER_ID, userId);
        return true;
    }

    private Long verify(String token) {
        if (token == null) {
            return null;
        }
        token = token.trim();
        if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH) {
            return null;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            return null;
        }
        long userId;
        long expiresAt;
        try {
            userId = Long.parseLong(parts[0]);
            expiresAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (expiresAt <= System.currentTimeMillis() / 1000L) {
            return null;
        }
        byte[] expected = hmacHex(parts[0] + "." + parts[1]);
        if (expected == null
                || !MessageDigest.isEqual(expected, parts[2].toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        return userId;
    }

    private byte[] hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
