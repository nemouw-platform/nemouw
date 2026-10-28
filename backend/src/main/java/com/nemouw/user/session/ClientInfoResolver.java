package com.nemouw.user.session;

import com.nemouw.common.util.UserAgentParser;
import org.springframework.stereotype.Component;

import static com.nemouw.user.AuthConstants.MAX_USER_AGENT_LENGTH;

@Component
class ClientInfoResolver {

    ClientInfo resolve(String ip, String rawUserAgent) {
        var parsed = UserAgentParser.parse(rawUserAgent);
        String ua = rawUserAgent == null ? null
                : (rawUserAgent.length() > MAX_USER_AGENT_LENGTH ? rawUserAgent.substring(0, MAX_USER_AGENT_LENGTH) : rawUserAgent);
        return new ClientInfo(ip, ua, parsed.deviceType(), parsed.osName(), parsed.browser());
    }
}