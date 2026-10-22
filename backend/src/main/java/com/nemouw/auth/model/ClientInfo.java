package com.nemouw.auth.model;

public record ClientInfo(String ip, String userAgent, String deviceType, String osName, String browser) {
}