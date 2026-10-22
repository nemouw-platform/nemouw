package com.nemouw.config;

import com.nemouw.auth.service.ClientInfoArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final ClientInfoArgumentResolver clientInfoResolver;

    public WebConfig(ClientInfoArgumentResolver clientInfoResolver) {
        this.clientInfoResolver = clientInfoResolver;
    }

    /** The old path("/v1", ...): every @RestController is served under /v1. */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/v1", type -> type.isAnnotationPresent(RestController.class));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clientInfoResolver);
    }
}