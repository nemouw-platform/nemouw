package com.nemouw;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class Config {
    private static final Logger logger = LogManager.getLogger(Config.class);
    private static final Dotenv ENV = Dotenv.configure().ignoreIfMissing().load();

    private static final String DB_URL = "DB_URL";
    private static final String JWT_SECRET = "JWT_SECRET";
    private static final String OTP_HASH_SECRET = "OTP_HASH_SECRET";
    private static final List<String> REQUIRED = List.of(DB_URL, JWT_SECRET, OTP_HASH_SECRET);

    private Config() {
    }

    /**
     * Call once at startup: reports every missing required variable together.
     */
    public static void validate() {
        List<String> missing = REQUIRED.stream()
                .filter(k -> {
                    String v = ENV.get(k);
                    return v == null || v.isBlank();
                })
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required environment variables: " + String.join(", ", missing));
        }
    }

    /**
     * For secrets and connection strings: the app refuses to start without them.
     */
    public static String require(String key) {
        String v = ENV.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return v;
    }

    /**
     * For tunable settings that have a sensible default.
     */
    public static int getInt(String key, int defaultValue) {
        String v = ENV.get(key);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            logger.warn("Environment variable {} is not a number ({}), using default {}", key, v, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String v = ENV.get(key);
        return (v == null || v.isBlank()) ? defaultValue : Boolean.parseBoolean(v.trim());
    }

    public static String dbUrl() {
        return require(DB_URL);
    }

    public static String jwtSecret() {
        return require(JWT_SECRET);
    }

    public static String otpHashSecret() {
        return require(OTP_HASH_SECRET);
    }

    public static boolean trustProxy() {
        return getBoolean("TRUST_PROXY", false);
    }

    public static boolean cookieSecure()        { return getBoolean("COOKIE_SECURE", true); }

    public static boolean allowLocalhostOrigins() { return getBoolean("ALLOW_LOCALHOST_ORIGINS", false); }

    public static int getDdosShieldRequestLimitPerSecond() {
        return getInt("DDOS_SHIELD_REQUEST_LIMIT_PER_SECOND", 5);
    }

    public static java.util.Set<String> trustedProxyIps() {
        String v = ENV.get("TRUSTED_PROXY_IPS");
        if (v == null || v.isBlank()) return java.util.Set.of();
        return java.util.Arrays.stream(v.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}