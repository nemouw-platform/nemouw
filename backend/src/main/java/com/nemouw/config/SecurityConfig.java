package com.nemouw.config;

import com.nemouw.enums.UserRole;
import com.nemouw.web.*;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.nemouw.auth.AuthConstants.CLAIM_ROLE;
import static com.nemouw.auth.AuthConstants.PASSWORD_SETUP_SCOPE;
import static com.nemouw.auth.service.AuthCookies.ACCESS;

/**
 * Three chains, tried in order; the first whose matcher fits the request handles it:
 * 1. public auth endpoints: no token is ever read, so a stale cookie cannot block login, refresh or logout;
 * 2. the API (/v1/**): a valid, live session is required (and a password-setup token for one endpoint);
 * 3. everything else: denied, except robots.txt and /error. Deny by default.
 * Every chain is stateless, has CORS and the rate limiter, and has no Spring CSRF filter: a cross-site browser
 * request carries an Origin header that the CORS filter rejects unless it is in app.cors.allowed-origins.
 */
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
    SecurityFilterChain publicAuthChain(HttpSecurity http, RateLimiter limiter, ClientIpResolver clientIp,
                                        SecurityErrorHandler errors) {
        applyStatelessDefaults(http, new RateLimitFilter(limiter, clientIp, errors));

        return http
                .securityMatcher(PUBLIC_AUTH)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http, RateLimiter limiter, ClientIpResolver clientIp,
                                 SecurityErrorHandler errors, JwtDecoder jwtDecoder) {
        applyStatelessDefaults(http, new RateLimitFilter(limiter, clientIp, errors));

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
                .addFilterAfter(new UserIdMdcFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }

    /** Safety net: anything that is not an API or public auth route (actuator, typos, future additions) is closed. */
    @Bean
    @Order(3)
    SecurityFilterChain fallbackChain(HttpSecurity http, RateLimiter limiter, ClientIpResolver clientIp,
                                      SecurityErrorHandler errors) {
        applyStatelessDefaults(http, new RateLimitFilter(limiter, clientIp, errors));

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/robots.txt").permitAll()
                        .requestMatchers("/error").permitAll()      // keeps the original status of container-level errors
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .build();
    }

    private static void applyStatelessDefaults(HttpSecurity http, RateLimitFilter rateLimit) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterAfter(rateLimit, CorsFilter.class);   // after CORS, so a 429 still carries CORS headers
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
        config.setExposedHeaders(List.of("Retry-After", TraceIdFilter.HEADER));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}