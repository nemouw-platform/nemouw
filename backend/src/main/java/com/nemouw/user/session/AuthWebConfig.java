package com.nemouw.user.session;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration(proxyBeanMethods = false)
class AuthWebConfig implements WebMvcConfigurer {
    private final ClientInfoArgumentResolver clientInfoResolver;

    AuthWebConfig(ClientInfoArgumentResolver clientInfoResolver) {
        this.clientInfoResolver = clientInfoResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clientInfoResolver);
    }
}