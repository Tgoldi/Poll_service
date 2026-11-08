package com.example.pollservice.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or a whole controller) as an admin-only action.
 * The request must carry the admin token (header X-Admin-Token) that matches
 * the POLL_ADMIN_TOKEN setting. While POLL_ADMIN_TOKEN is not set, these
 * actions are refused for everyone.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
}
