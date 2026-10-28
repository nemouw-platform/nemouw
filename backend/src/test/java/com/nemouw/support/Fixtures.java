package com.nemouw.support;

import com.nemouw.user.session.ClientInfo;
import com.nemouw.common.config.AppProperties;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Realistic, fixed values shared by the tests. Nothing here is random, so every test is repeatable. */
public final class Fixtures {
    private Fixtures() {
    }

    /** 64 characters, long enough to sign HS512 as well, which lets tests forge a wrong-algorithm token. */
    public static final String JWT_SECRET = "Vb4Nq8Xz2Kp7Lm1Tc6Hd9Rf3Wj5Ys0Ga-nemouw-jwt-signing-key-2026-hs512";
    public static final String OTP_SECRET = "Qe7Tn2Wb9Ux4Zk6Pm1Rv8Hc3Yd5Lf0Aj-nemouw-otp-secret-key-2026";
    public static final String OTHER_SECRET = "Another-otp-secret-key-for-rotation-tests-2026!";

    public static final Instant NOW = Instant.parse("2026-10-24T09:15:30Z");

    public static final UUID USER_ID = UUID.fromString("3f2b8c1e-5a47-4d6e-9b0a-7c1d2e8f4a90");
    public static final UUID OTHER_USER_ID = UUID.fromString("b71c40aa-9d3e-4f58-a2c6-18e5d0f7b3c4");
    public static final UUID IDENTIFIER_ID = UUID.fromString("c92e5d17-6b08-4a3f-8e41-d5a7f2b9c630");
    public static final UUID SESSION_ID = UUID.fromString("7a1d9e02-4c55-4b8a-9f3e-60b2c8d41e75");
    public static final UUID TOKEN_ID = UUID.fromString("e4086f3b-2d91-47c7-b5a0-9c3d71e8a2f6");

    public static final String EMAIL = "amira.haddad@clinicmail.org";
    public static final String EMAIL_LOCAL_PART = "amira.haddad";
    public static final String PASSWORD = "vK9#mQ2$xL7!pR4@";

    public static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static AppProperties props() {
        return props(true, false);
    }

    public static AppProperties props(boolean cookieSecure, boolean trustProxy) {
        return new AppProperties(
                new AppProperties.Security(JWT_SECRET, OTP_SECRET, cookieSecure, trustProxy, 900, 90),
                new AppProperties.Cors(List.of("https://app.nemouw.com"), false),
                new AppProperties.RateLimit(20),
                new AppProperties.Mail("re_live_7Hq2Zk9Xw4", "Nemouw <no-reply@nemouw.com>", "https://app.nemouw.com"));
    }

    public static ClientInfo clientInfo() {
        return new ClientInfo("203.0.113.7",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 Version/17.5 Safari/605.1.15",
                "desktop", "macOS", "Safari");
    }

    /** Fails unless the call throws an ApiException carrying exactly this error code. */
    public static void assertApiError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.errorCode()).isEqualTo(expected));
    }

    /** A clock the test can move forward. */
    public static final class MutableClock extends Clock {
        private Instant now;

        public MutableClock(Instant start) {
            this.now = start;
        }

        public void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /**
     * A real TransactionTemplate over a manager that touches no database but counts how each transaction ended,
     * so tests can tell "committed" from "rolled back" (for example: a wrong OTP must keep its spent attempt).
     */
    public static final class RecordingTransactions {
        private int commits;
        private int rollbacks;
        private final TransactionTemplate template = new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                commits++;
            }

            @Override
            public void rollback(TransactionStatus status) {
                rollbacks++;
            }
        });

        public TransactionTemplate template() {
            return template;
        }

        public int commits() {
            return commits;
        }

        public int rollbacks() {
            return rollbacks;
        }
    }
}