package com.nemouw.http;

import com.nemouw.Config;
import spark.Request;

public final class ClientIp {
    private ClientIp() {
    }

    public static String of(Request req) {
        String ip = null;

        if (Config.trustProxy()) {
            String forwarded = req.headers("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // The last entry is the address the trusted proxy saw.
                // Earlier entries are client-supplied and can be spoofed.
                String[] parts = forwarded.split(",");
                ip = parts[parts.length - 1].trim();
            }
        }

        if (ip == null || ip.isEmpty()) {
            ip = req.ip();
        }

        int zone = ip.indexOf('%');   // strip IPv6 zone id (fe80::1%eth0)
        return zone >= 0 ? ip.substring(0, zone) : ip;
    }
}