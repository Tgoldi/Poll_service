package com.example.pollservice.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a read action that exposes other users' data. It stays public while
 * POLL_ADMIN_TOKEN is not set (today's behaviour) and requires the admin token
 * (header X-Admin-Token) as soon as POLL_ADMIN_TOKEN is set.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedRead {
}
