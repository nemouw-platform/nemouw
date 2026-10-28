package com.nemouw.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Validation runs at startup and reports every violation together, so the app refuses to start
 * with missing or weak secrets.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Security security,
        @Valid @NotNull Cors cors,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Mail mail) {

    public record Security(
            @NotBlank @Size(min = 32) String jwtSecret,
            @NotBlank @Size(min = 32) String otpHashSecret,
            boolean cookieSecure,
            boolean trustProxy,
            @Min(1) @DefaultValue("900") int accessTtlSeconds,
            @Min(1) @DefaultValue("90") int refreshTtlDays
    ) {}

    public record Cors(
            @NotEmpty List<String> allowedOrigins,
            boolean allowLocalhostOrigins) {
    }

    public record RateLimit(@Min(1) int requestsPerSecond) {
    }

    public record Mail(
            @NotBlank String resendApiKey,
            @NotBlank String from,
            @NotBlank String urlBase) {
    }
}