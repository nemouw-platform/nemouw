package com.nemouw.auth.service;

import com.nemouw.auth.model.ClientInfo;
import com.nemouw.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets a controller method simply declare a {@link ClientInfo} parameter and receive the caller's IP and device. */
@Component
public class ClientInfoArgumentResolver implements HandlerMethodArgumentResolver {
    private final ClientIpResolver clientIp;
    private final ClientInfoResolver clientInfo;

    public ClientInfoArgumentResolver(ClientIpResolver clientIp, ClientInfoResolver clientInfo) {
        this.clientIp = clientIp;
        this.clientInfo = clientInfo;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ClientInfo.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        assert request != null;
        return clientInfo.resolve(clientIp.resolve(request), request.getHeader(HttpHeaders.USER_AGENT));
    }
}