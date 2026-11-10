package com.example.pollservice.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method that acts on behalf of a poll respondent.
 * When POLL_USER_SIGNING_KEY is set, {@link RespondentTokenInterceptor} requires a
 * signed X-User-Token header and exposes the verified user id as the request
 * attribute {@link RespondentTokenInterceptor#VERIFIED_USER_ID}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RespondentIdentity {
}
