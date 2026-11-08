package com.example.pollservice.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method that records a vote in a user's name.
 * {@link VoteGuardInterceptor} rate-limits it per client address and, once
 * POLL_USER_TOKEN_SECRET is set, requires a signed user identity (headers
 * X-User-Id and X-User-Token) and puts the verified user id in the request
 * attribute {@link VoteGuardInterceptor#VERIFIED_USER_ID}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CastsVote {
}
