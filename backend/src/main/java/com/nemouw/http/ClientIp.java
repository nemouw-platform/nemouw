package com.nemouw.http;

import com.nemouw.Config;
import spark.Request;

import java.net.InetAddress;
import java.util.Set;
import java.util.regex.Pattern;

public final class ClientIp {
    private ClientIp() {}

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4 = Pattern.compile("^(" + OCTET + "\\.){3}" + OCTET + "$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    /** Extra proxies to trust, e.g. a load balancer with a public address: TRUSTED_PROXY_IPS=203.0.113.5,203.0.113.6 */
    private static final Set<String> EXTRA_TRUSTED = Config.trustedProxyIps();

    public static String resolve(Request req) {
        return resolve(req.ip(), req.headers("X-Forwarded-For"));
    }

    /** Pure function so it can be unit tested without Spark. */
    static String resolve(String rawPeer, String xForwardedFor) {
        String peer = literalOrNull(rawPeer);
        if (peer == null) return null;
        if (xForwardedFor == null || xForwardedFor.isBlank() || !isTrustedProxy(peer)) return peer;

        String[] hops = xForwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = literalOrNull(hops[i]);
            if (hop == null) return peer;              // junk in the chain: don't guess
            if (!isTrustedProxy(hop)) return hop;      // first untrusted from the right = the real client
        }
        String first = literalOrNull(hops[0]);         // every hop was internal (e.g. local Docker)
        return first != null ? first : peer;
    }

    /** Returns a normalized IP literal, or null. Never does a DNS lookup: only valid literals reach InetAddress. */
    private static String literalOrNull(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int zone = s.indexOf('%');                     // "fe80::1%eth0" is not a valid inet value
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

    private static boolean isTrustedProxy(String ip) {
        if (EXTRA_TRUSTED.contains(ip)) return true;
        try {
            InetAddress a = InetAddress.getByName(ip);
            if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()) return true;
            byte[] b = a.getAddress();
            return b.length == 16 && (b[0] & 0xfe) == 0xfc;      // IPv6 unique-local fc00::/7
        } catch (Exception e) {
            return false;
        }
    }
}