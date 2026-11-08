package com.example.pollservice.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebAuthConfig implements WebMvcConfigurer {

    private final AdminTokenInterceptor adminTokenInterceptor;
    private final VoteGuardInterceptor voteGuardInterceptor;

    public WebAuthConfig(AdminTokenInterceptor adminTokenInterceptor, VoteGuardInterceptor voteGuardInterceptor) {
        this.adminTokenInterceptor = adminTokenInterceptor;
        this.voteGuardInterceptor = voteGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminTokenInterceptor);
        registry.addInterceptor(voteGuardInterceptor);
    }
}
