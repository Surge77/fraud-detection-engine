package com.fraudengine.config;

import com.fraudengine.api.AdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the admin-token guard on the admin endpoint path only. The
 * interceptor is constructed here (not a component) so it picks up the token
 * property and stays out of the way of non-admin routes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String adminToken;

    public WebConfig(@Value("${fraud.admin.token}") String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(adminToken))
                .addPathPatterns("/api/v1/admin/**");
    }
}
