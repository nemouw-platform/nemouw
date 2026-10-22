package com.nemouw.auth.service;

import com.nemouw.auth.model.ClientInfo;
import com.nemouw.util.UserAgentParser;
import org.springframework.stereotype.Component;

import static com.nemouw.auth.AuthConstants.MAX_USER_AGENT_LENGTH;

@Component
public class ClientInfoResolver {

    public ClientInfo resolve(String ip, String rawUserAgent) {
        var parsed = UserAgentParser.parse(rawUserAgent);
        String ua = rawUserAgent == null ? null
                : (rawUserAgent.length() > MAX_USER_AGENT_LENGTH ? rawUserAgent.substring(0, MAX_USER_AGENT_LENGTH) : rawUserAgent);
        return new ClientInfo(ip, ua, parsed.deviceType(), parsed.osName(), parsed.browser());
    }
}