package com.nemouw.auth.service;

import com.nemouw.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static com.nemouw.auth.AuthConstants.*;

/** Generates OTP codes and reset tokens, and hashes them (HMAC) before they are stored. */
@Service
public class VerificationTokenService {
    private static final String OTP_FORMAT = "%0" + OTP_CODE_DIGITS + "d";
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public VerificationTokenService(AppProperties props) {
        this.key = props.security().otpHashSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String generateCode() {
        return String.format(OTP_FORMAT, random.nextInt(OTP_CODE_SPACE));
    }

    public String generateResetToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(UUID userId, String code) {
        return hmac(userId + ":" + code);
    }

    public String hashToken(String token) {
        return hmac("reset-token:" + token);
    }

    public boolean matches(UUID userId, String code, String storedHash) {
        return MessageDigest.isEqual(
                hash(userId, code).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}