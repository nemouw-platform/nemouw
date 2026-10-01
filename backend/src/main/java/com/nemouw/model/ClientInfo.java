package com.nemouw.model;

import com.nemouw.util.UserAgentParser;

public record ClientInfo(String ip, String userAgent, String deviceType, String osName, String browser) {
    private static final int MAX_UA_LENGTH = 512;   // the header can be 8 KB: don't store attacker-sized text

    public static ClientInfo from(String ip, String rawUserAgent) {
        var parsed = UserAgentParser.parse(rawUserAgent);
        String ua = rawUserAgent == null ? null
                : (rawUserAgent.length() > MAX_UA_LENGTH ? rawUserAgent.substring(0, MAX_UA_LENGTH) : rawUserAgent);
        return new ClientInfo(ip, ua, parsed.deviceType(), parsed.osName(), parsed.browser());
    }
}