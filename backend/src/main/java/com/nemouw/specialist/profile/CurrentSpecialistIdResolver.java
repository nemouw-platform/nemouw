package com.nemouw.specialist.profile;

import com.nemouw.common.exception.Errors;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Objects;
import java.util.UUID;

@Component
class CurrentSpecialistIdResolver implements HandlerMethodArgumentResolver {
    private final SpecialistLookup specialists;

    CurrentSpecialistIdResolver(SpecialistLookup specialists) {
        this.specialists = specialists;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentSpecialistId.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth)) {
            throw Errors.missingAccessToken();
        }
        UUID userId = UUID.fromString(Objects.requireNonNull(auth.getToken().getSubject()));
        return specialists.requireIdOf(userId);
    }
}