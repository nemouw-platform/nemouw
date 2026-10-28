package com.nemouw.security;

import com.nemouw.common.config.AppProperties;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.web.ClientIpResolver;
import com.nemouw.common.web.TraceIdFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.nemouw.user.AuthConstants.CLAIM_ROLE;
import static com.nemouw.user.AuthConstants.PASSWORD_SETUP_SCOPE;
import static com.nemouw.user.session.AuthCookies.ACCESS;
import static com.nemouw.common.ApiPaths.API_PREFIX;

/**
 * Four stateless filter chains; the first whose matcher fits handles the request:
 * 1. public auth endpoints and 2. public reads: no token is read, so a stale cookie cannot block them;
 * 3. the API: a live session is required. Role rules per resource live here and fail closed (a new module
 *    must add its rule); rules that need data (e.g. "staff of THIS syndicate") are @PreAuthorize on the endpoint;
 * 4. everything else is denied.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String API = API_PREFIX;

    private static final String ADMIN = UserRole.ADMIN.code();
    private static final String STAFF = UserRole.SYNDICATE_STAFF.code();

    private static final String[] PUBLIC_AUTH = {
            API + "/auth/signup", API + "/auth/verify-otp", API + "/auth/resend-otp",
            API + "/auth/login", API + "/auth/refresh", API + "/auth/logout",
            API + "/auth/password/forgot", API + "/auth/password/reset"};

    private static final String PASSWORD_SETUP = API + "/auth/password/setup";

    private static final String[] PUBLIC_READS = {
            API + "/countries/**", API + "/specialties/**", API + "/conditions/**",
            API + "/syndicates", API + "/syndicates/*"};

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
    SecurityFilterChain publicReadChain(HttpSecurity http, RateLimiter limiter, ClientIpResolver clientIp,
                                        SecurityErrorHandler errors) {
        applyStatelessDefaults(http, new RateLimitFilter(limiter, clientIp, errors));

        var path = PathPatternRequestMatcher.withDefaults();
        RequestMatcher[] reads = Arrays.stream(PUBLIC_READS)
                .map(p -> path.matcher(HttpMethod.GET, p))
                .toArray(RequestMatcher[]::new);

        return http
                .securityMatcher(new OrRequestMatcher(reads))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain apiChain(HttpSecurity http, RateLimiter limiter, ClientIpResolver clientIp,
                                 SecurityErrorHandler errors, JwtDecoder jwtDecoder) {
        applyStatelessDefaults(http, new RateLimitFilter(limiter, clientIp, errors));

        return http
                .securityMatcher(API + "/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PASSWORD_SETUP).hasAuthority("SCOPE_" + PASSWORD_SETUP_SCOPE)
                        .requestMatchers(HttpMethod.DELETE, API + "/conditions/**").hasRole(ADMIN)
                        .requestMatchers(API + "/conditions/**").hasAnyRole(ADMIN, STAFF)
                        .requestMatchers(API + "/countries/**", API + "/specialties/**").hasRole(ADMIN)
                        .requestMatchers(API + "/syndicates/**").hasAnyRole(ADMIN, STAFF)
                        .anyRequest().denyAll())
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

    @Bean
    @Order(4)
    SecurityFilterChain fallbackChain(HttpSecurity http, RateLimiter limiter, ClientIpResolver clientIp,
                                      SecurityErrorHandler errors) {
        applyStatelessDefaults(http, new RateLimitFilter(limiter, clientIp, errors));

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/robots.txt").permitAll()
                        .requestMatchers("/error").permitAll()   // keeps the original status of container-level errors
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