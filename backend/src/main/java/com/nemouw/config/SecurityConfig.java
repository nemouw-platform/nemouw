package com.nemouw.config;

import com.nemouw.enums.UserRole;
import com.nemouw.web.SecurityErrorHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.nemouw.auth.AuthConstants.CLAIM_ROLE;
import static com.nemouw.auth.AuthConstants.PASSWORD_SETUP_SCOPE;
import static com.nemouw.auth.service.AuthCookies.ACCESS;


@Configuration
public class SecurityConfig {
    private static final String[] PUBLIC_AUTH = {
            "/v1/auth/signup", "/v1/auth/verify-otp", "/v1/auth/resend-otp",
            "/v1/auth/login", "/v1/auth/refresh", "/v1/auth/logout",
            "/v1/auth/password/forgot", "/v1/auth/password/reset"};

    private static final String PASSWORD_SETUP = "/v1/auth/password/setup";

    private static final String[] ALL_ROLES = Arrays.stream(UserRole.values())
            .map(UserRole::code).toArray(String[]::new);

    @Bean
    @Order(1)
    SecurityFilterChain publicAuthChain(HttpSecurity http) throws Exception {
        applyStatelessDefaults(http);

        return http
                .securityMatcher(PUBLIC_AUTH)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(
            HttpSecurity http,
            SecurityErrorHandler errors,
            org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder) throws Exception {

        applyStatelessDefaults(http);

        return http
                .securityMatcher("/v1/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PASSWORD_SETUP)
                        .hasAuthority("SCOPE_" + PASSWORD_SETUP_SCOPE)
                        .anyRequest().hasAnyRole(ALL_ROLES))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(SecurityConfig::accessTokenFromCookie)
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(authoritiesConverter())))
                .build();
    }

    private static void applyStatelessDefaults(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
    }

    private static String accessTokenFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, ACCESS);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }

    private static JwtAuthenticationConverter authoritiesConverter() {
        var scopes = new JwtGrantedAuthoritiesConverter();
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(CLAIM_ROLE);
        roles.setAuthorityPrefix("ROLE_");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Stream
                .concat(scopes.convert(jwt).stream(), roles.convert(jwt).stream())
                .toList());
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        AppProperties.Cors cors = props.cors();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(cors.allowedOrigins());
        if (cors.allowLocalhostOrigins()) {
            config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}