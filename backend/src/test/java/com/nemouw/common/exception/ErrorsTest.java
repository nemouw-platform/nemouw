package com.nemouw.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** These codes and statuses are the public API contract: a client branches on them, so changing one is a breaking change. */
class ErrorsTest {

    static Stream<Arguments> contract() {
        return Stream.of(
                Arguments.of(Errors.invalidCredentials(), "INVALID_CREDENTIALS", 401),
                Arguments.of(Errors.invalidOrExpiredCode(), "INVALID_OR_EXPIRED_TOKEN", 400),
                Arguments.of(Errors.emailTaken(), "EMAIL_TAKEN", 409),
                Arguments.of(Errors.accountNotVerified(), "ACCOUNT_NOT_VERIFIED", 403),
                Arguments.of(Errors.accountSuspended(), "ACCOUNT_SUSPENDED", 403),
                Arguments.of(Errors.invalidRefreshToken(), "INVALID_TOKEN", 401),
                Arguments.of(Errors.missingAccessToken(), "UNAUTHENTICATED", 401),
                Arguments.of(Errors.invalidAccessToken(), "INVALID_TOKEN", 401),
                Arguments.of(Errors.forbidden(), "FORBIDDEN", 403),
                Arguments.of(Errors.csrfBlocked(), "CSRF_BLOCKED", 403),
                Arguments.of(Errors.malformedJson("Unexpected end of input"), "MALFORMED_JSON", 400),
                Arguments.of(Errors.tooManyRequests(30), "TOO_MANY_REQUESTS", 429),
                Arguments.of(new ValidationException(List.of(FieldErrors.required("email"))), "VALIDATION_ERROR", 422));
    }

    @ParameterizedTest
    @MethodSource("contract")
    void exposesTheDocumentedCodeAndHttpStatus(ApiException error, String code, int status) {
        assertThat(error.code()).isEqualTo(code);
        assertThat(error.status()).isEqualTo(status);
    }

    @Test
    void usesTheSameMessageForAWrongPasswordAndAnUnknownAccountSoNeitherIsRevealed() {
        assertThat(Errors.invalidCredentials().getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    void tellsTheClientHowLongToWaitThroughTheRetryAfterHeader() {
        TooManyRequestsException error = Errors.tooManyRequests(30);

        assertThat(error.responseHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(error.retryAfterSeconds()).isEqualTo(30);
    }

    @Test
    void carriesNoStackTraceBecauseTheseAreExpectedOutcomesAndNotBugs() {
        assertThat(Errors.emailTaken().getStackTrace()).isEmpty();
    }

    @Test
    void keepsTheFieldErrorsItWasCreatedWithEvenIfTheSourceListChangesLater() {
        List<FieldError> source = new ArrayList<>(List.of(FieldErrors.required("email")));
        ValidationException error = new ValidationException(source);

        source.add(FieldErrors.required("password"));

        assertThat(error.fieldErrors()).containsExactly(FieldErrors.required("email"));
    }

    @Test
    void describesFieldErrorsWithTheirFieldCodeAndAHumanMessage() {
        assertThat(FieldErrors.tooShort("password", 8))
                .isEqualTo(new FieldError("password", FieldErrorCode.TOO_SHORT, "must be at least 8 characters"));
        assertThat(FieldErrors.tooLong("first_name", 100))
                .isEqualTo(new FieldError("first_name", FieldErrorCode.TOO_LONG, "must be at most 100 characters"));
        assertThat(FieldErrors.required("email"))
                .isEqualTo(new FieldError("email", FieldErrorCode.REQUIRED, "is required"));
    }
}
