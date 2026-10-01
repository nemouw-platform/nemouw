package com.nemouw.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

import static com.nemouw.Constants.*;

public class OtpService {
    private static final String OTP_FORMAT = "%0" + OTP_CODE_DIGITS + "d";
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public OtpService(String hashSecret) {
        if (hashSecret == null || hashSecret.length() < MIN_SECRET_LENGTH)
            throw new IllegalStateException("OTP_HASH_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
        this.key = hashSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateCode() {
        return String.format(OTP_FORMAT, random.nextInt(OTP_CODE_SPACE));
    }

    public String hash(UUID userId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(UUID userId, String code, String storedHash) {
        return MessageDigest.isEqual(
                hash(userId, code).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}