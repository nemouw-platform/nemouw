package com.nemouw.user.signup;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.user.signup")
record SignupProperties(
        @NotNull Duration setupTtl) {
}