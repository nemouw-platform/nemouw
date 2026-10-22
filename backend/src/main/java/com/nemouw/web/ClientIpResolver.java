package com.nemouw.web;

import com.nemouw.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * Finds the real client IP: X-Forwarded-For is only believed when
 * TRUST_PROXY is on AND the direct peer is a private/loopback address (i.e. your own nginx).
 * Do not set server.forward-headers-strategy: it would let Tomcat rewrite the address with different rules.
 */
@Component
public class ClientIpResolver {
    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4 = Pattern.compile("^(" + OCTET + "\\.){3}" + OCTET + "$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private final boolean trustProxy;

    public ClientIpResolver(AppProperties props) {
        this.trustProxy = props.security().trustProxy();
    }

    public String resolve(HttpServletRequest request) {
        return resolve(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), trustProxy);
    }

    static String resolve(String rawPeer, String xForwardedFor, boolean trustProxy) {
        String peer = literalOrNull(rawPeer);
        if (peer == null) return null;
        if (!trustProxy || xForwardedFor == null || xForwardedFor.isBlank() || !isNginxPeer(peer)) {
            return peer;
        }

        // nginx appends the address it saw, so the last entry is the real client.
        // Earlier entries are client-supplied and must not be trusted.
        String[] hops = xForwardedFor.split(",");
        String last = literalOrNull(hops[hops.length - 1]);
        return last != null ? last : peer;
    }

    /** Returns a normalized IP literal, or null. Never does a DNS lookup. */
    private static String literalOrNull(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int zone = s.indexOf('%');
        if (zone >= 0) s = s.substring(0, zone);
        if (IPV4.matcher(s).matches()) return s;
        if (s.contains(":") && IPV6_CHARS.matcher(s).matches()) {
            try {
                return InetAddress.getByName(s).getHostAddress();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static boolean isNginxPeer(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()) return true;
            byte[] b = a.getAddress();
            return b.length == 16 && (b[0] & 0xfe) == 0xfc;
        } catch (Exception e) {
            return false;
        }
    }
}