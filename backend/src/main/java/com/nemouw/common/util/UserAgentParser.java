package com.nemouw.common.util;

import java.util.Locale;

public final class UserAgentParser {
    private UserAgentParser() {}

    public record Result(String deviceType, String osName, String browser) {}

    private static final Result UNKNOWN = new Result("unknown", "Unknown", "Unknown");

    public static Result parse(String ua) {
        if (ua == null || ua.isBlank()) return UNKNOWN;
        String s = ua.length() > 512 ? ua.substring(0, 512) : ua;
        return new Result(deviceType(s), os(s), browser(s));
    }

    private static String browser(String s) {
        if (s.startsWith("curl/")) return "curl";
        if (s.startsWith("PostmanRuntime")) return "Postman";
        // Order matters: Edge, Opera and Samsung all also contain "Chrome" and "Safari"
        if (has(s, "Edg/", "EdgA/", "EdgiOS/", "Edge/")) return "Edge";
        if (has(s, "OPR/", "Opera")) return "Opera";
        if (s.contains("SamsungBrowser")) return "Samsung Internet";
        if (has(s, "Firefox/", "FxiOS")) return "Firefox";
        if (has(s, "CriOS", "Chrome/", "Chromium")) return "Chrome";
        if (s.contains("Safari/")) return "Safari";
        return "Unknown";
    }

    private static String os(String s) {
        if (s.contains("Windows")) return "Windows";
        // iOS before macOS: iPhone/iPad user agents also say "like Mac OS X".
        // Note: iPadOS Safari now sends a Macintosh UA, so those iPads show up as macOS.
        if (has(s, "iPhone", "iPad", "iPod")) return "iOS";
        // Android before Linux: Android user agents also say "Linux"
        if (s.contains("Android")) return "Android";
        if (s.contains("CrOS")) return "ChromeOS";
        if (has(s, "Mac OS X", "Macintosh")) return "macOS";
        if (has(s, "Linux", "X11")) return "Linux";
        return "Unknown";
    }

    private static String deviceType(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        if (has(lower, "bot", "crawler", "spider", "slurp")) return "bot";
        if (s.contains("iPad") || lower.contains("tablet")) return "tablet";
        if (s.contains("Android")) return s.contains("Mobile") ? "mobile" : "tablet";
        if (has(s, "iPhone", "iPod", "Mobile")) return "mobile";
        return switch (os(s)) {
            case "Windows", "macOS", "Linux", "ChromeOS" -> "desktop";
            default -> "unknown";
        };
    }

    private static boolean has(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }
}