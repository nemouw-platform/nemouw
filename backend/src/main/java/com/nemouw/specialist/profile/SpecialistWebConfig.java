package com.nemouw.specialist.profile;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration(proxyBeanMethods = false)
class SpecialistWebConfig implements WebMvcConfigurer {
    private final CurrentSpecialistIdResolver currentSpecialistId;

    SpecialistWebConfig(CurrentSpecialistIdResolver currentSpecialistId) {
        this.currentSpecialistId = currentSpecialistId;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentSpecialistId);
    }
}