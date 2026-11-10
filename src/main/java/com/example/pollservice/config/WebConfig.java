package com.example.pollservice.config;

import com.example.pollservice.security.AdminTokenInterceptor;
import com.example.pollservice.security.RespondentTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminTokenInterceptor adminTokenInterceptor;
    private final RespondentTokenInterceptor respondentTokenInterceptor;

    public WebConfig(AdminTokenInterceptor adminTokenInterceptor,
                     RespondentTokenInterceptor respondentTokenInterceptor) {
        this.adminTokenInterceptor = adminTokenInterceptor;
        this.respondentTokenInterceptor = respondentTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminTokenInterceptor);
        registry.addInterceptor(respondentTokenInterceptor);
    }
}
