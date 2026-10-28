package com.nemouw.user.session;

public record ClientInfo(String ip, String userAgent, String deviceType, String osName, String browser) {
}