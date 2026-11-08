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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Guards every handler marked {@link CastsVote}:
 * <ul>
 *   <li>Always: at most POLL_RESPONSE_RATE_LIMIT_PER_MINUTE votes per client address per minute (default 300).</li>
 *   <li>Once POLL_USER_TOKEN_SECRET is set: the caller must prove who the user is with the headers
 *       X-User-Id (the user's numeric id) and X-User-Token ("expiresEpochSeconds.hexHmac", where hexHmac is
 *       the lowercase hex HMAC-SHA256 of "userId.expiresEpochSeconds" keyed with POLL_USER_TOKEN_SECRET).
 *       The verified id is then used instead of any user id sent in the body.</li>
 * </ul>
 */
@Component
public class VoteGuardInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VoteGuardInterceptor.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_TOKEN_HEADER = "X-User-Token";
    /** Request attribute holding the verified user id (a Long), set only when identity checks are on. */
    public static final String VERIFIED_USER_ID = VoteGuardInterceptor.class.getName() + ".verifiedUserId";

    private static final int DEFAULT_LIMIT_PER_MINUTE = 300;
    private static final int MAX_TRACKED_CLIENTS = 100_000;

    private final byte[] userTokenSecret;
    private final int limitPerMinute;

    /** client address -> {minute window, count in that window} */
    private final Map<String, long[]> windows = new HashMap<>();

    public VoteGuardInterceptor(@Value("${POLL_USER_TOKEN_SECRET:}") String userTokenSecret,
                                @Value("${POLL_RESPONSE_RATE_LIMIT_PER_MINUTE:}") String limitPerMinute) {
        if (userTokenSecret == null || userTokenSecret.isBlank()) {
            this.userTokenSecret = null;
            log.warn("POLL_USER_TOKEN_SECRET is not set: POST /responses accepts the userId sent in the body, so "
                    + "anyone can vote in another user's name. Set POLL_USER_TOKEN_SECRET and have your login "
                    + "system send " + USER_ID_HEADER + " and " + USER_TOKEN_HEADER + " with each vote.");
        } else {
            this.userTokenSecret = userTokenSecret.trim().getBytes(StandardCharsets.UTF_8);
        }
        this.limitPerMinute = parseLimit(limitPerMinute);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!handlerMethod.hasMethodAnnotation(CastsVote.class)
                && !AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), CastsVote.class)) {
            return true;
        }
        if (!allow(clientAddress(request))) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        if (userTokenSecret == null) {
            return true;
        }
        Long userId = verifiedUserId(request.getHeader(USER_ID_HEADER), request.getHeader(USER_TOKEN_HEADER));
        if (userId == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        request.setAttribute(VERIFIED_USER_ID, userId);
        return true;
    }

    /**
     * The address to rate-limit on. X-Forwarded-For is only read when the direct peer is a private or loopback
     * address (a reverse proxy or load balancer); then the right-most entry that is not such an address is the
     * client, so entries an internet client prepends itself cannot change its bucket.
     */
    private static String clientAddress(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (remote == null || !isPrivateOrLoopback(remote)) {
            return remote;
        }
        java.util.List<String> entries = new java.util.ArrayList<>();
        java.util.Enumeration<String> headers = request.getHeaders("X-Forwarded-For");
        while (headers != null && headers.hasMoreElements()) {
            String header = headers.nextElement();
            if (header == null) {
                continue;
            }
            for (String part : header.split(",")) {
                String entry = part.trim();
                if (!entry.isEmpty()) {
                    entries.add(entry.length() > 64 ? entry.substring(0, 64) : entry);
                }
            }
        }
        String client = remote;
        for (int i = entries.size() - 1; i >= 0; i--) {
            client = entries.get(i);
            if (!isPrivateOrLoopback(client)) {
                break;
            }
        }
        return client;
    }

    /** Classifies an address literal without any name lookup. */
    private static boolean isPrivateOrLoopback(String address) {
        String a = address.trim().toLowerCase();
        if (a.startsWith("[") && a.endsWith("]")) {
            a = a.substring(1, a.length() - 1);
        }
        if (a.startsWith("::ffff:")) {
            a = a.substring("::ffff:".length());
        }
        if (a.indexOf(':') >= 0) {
            return a.equals("::1") || a.equals("0:0:0:0:0:0:0:1")
                    || a.startsWith("fc") || a.startsWith("fd") || a.startsWith("fe80:");
        }
        String[] octets = a.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        int[] o = new int[4];
        for (int i = 0; i < 4; i++) {
            if (octets[i].isEmpty() || octets[i].length() > 3 || !octets[i].chars().allMatch(Character::isDigit)) {
                return false;
            }
            o[i] = Integer.parseInt(octets[i]);
            if (o[i] > 255) {
                return false;
            }
        }
        return o[0] == 10 || o[0] == 127
                || (o[0] == 172 && o[1] >= 16 && o[1] <= 31)
                || (o[0] == 192 && o[1] == 168)
                || (o[0] == 169 && o[1] == 254)
                || (o[0] == 100 && o[1] >= 64 && o[1] <= 127);
    }

    private Long verifiedUserId(String userIdHeader, String tokenHeader) {
        if (userIdHeader == null || tokenHeader == null) {
            return null;
        }
        long userId;
        long expires;
        String[] parts = tokenHeader.trim().split("\\.", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            userId = Long.parseLong(userIdHeader.trim());
            expires = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (expires <= System.currentTimeMillis() / 1000L) {
            return null;
        }
        byte[] expected = hmacHex(userId + "." + expires).getBytes(StandardCharsets.UTF_8);
        byte[] supplied = parts[1].toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, supplied) ? userId : null;
    }

    private String hmacHex(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(userTokenSecret, "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }

    private synchronized boolean allow(String client) {
        long minute = System.currentTimeMillis() / 60_000L;
        if (windows.size() >= MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> w[0] != minute);
            if (windows.size() >= MAX_TRACKED_CLIENTS) {
                windows.clear();
            }
        }
        long[] window = windows.computeIfAbsent(String.valueOf(client), k -> new long[]{minute, 0});
        if (window[0] != minute) {
            window[0] = minute;
            window[1] = 0;
        }
        window[1]++;
        return window[1] <= limitPerMinute;
    }

    private static int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LIMIT_PER_MINUTE;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : DEFAULT_LIMIT_PER_MINUTE;
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT_PER_MINUTE;
        }
    }
}
