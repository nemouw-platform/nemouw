package com.nemouw.common.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static com.nemouw.common.web.TraceIdFilter.MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private static final String TRACE_ID = "trace-123";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearTheLogContext() {
        MDC.clear();
    }

    private static void assertError(ResponseEntity<ErrorBody> response, int status, String code, String message) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        ErrorBody body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status);
        assertThat(body.code()).isEqualTo(code);
        assertThat(body.message()).isEqualTo(message);
    }

    private static org.springframework.validation.FieldError springFieldError(String field, String message,
                                                                              String... codes) {
        return new org.springframework.validation.FieldError("request", field, null, false, codes, null, message);
    }

    private static org.springframework.validation.FieldError constraintError(String field, String constraint,
                                                                             String message) {
        return springFieldError(field, message, constraint + ".request." + field, constraint + "." + field,
                constraint + ".java.lang.String", constraint);
    }

    private static BindException bindException(ObjectError... errors) {
        MapBindingResult result = new MapBindingResult(new HashMap<>(), "request");
        Arrays.stream(errors).forEach(result::addError);
        return new BindException(result);
    }

    private static HttpMessageNotReadableException unreadable(String message) {
        return new HttpMessageNotReadableException(message, new MockHttpInputMessage(new byte[0]));
    }

    @Nested
    class OwnExceptions {

        @Test
        void answersWithTheStatusCodeAndMessageOfTheException() {
            ResponseEntity<ErrorBody> response = handler.handleApi(Errors.invalidCredentials());

            assertError(response, 401, "INVALID_CREDENTIALS", "Invalid email or password");
            assertThat(response.getBody().errors()).isEmpty();
        }

        @Test
        void listsValidationErrorsInTheirOriginalOrder() {
            List<FieldError> errors = List.of(FieldErrors.required("email"), FieldErrors.tooShort("password", 8),
                    FieldErrors.required("first_name"));

            ResponseEntity<ErrorBody> response = handler.handleApi(new ValidationException(errors));

            assertError(response, 422, "VALIDATION_ERROR", "Validation failed");
            assertThat(response.getBody().errors()).containsExactlyElementsOf(errors);
        }

        @Test
        void passesTheRetryAfterHeaderThrough() {
            ResponseEntity<ErrorBody> response = handler.handleApi(Errors.tooManyRequests(30));

            assertError(response, 429, "TOO_MANY_REQUESTS", ErrorCode.TOO_MANY_REQUESTS.defaultMessage());
            assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("30");
        }

        @Test
        void addsNoHeadersBeyondTheContentTypeForAPlainException() {
            ResponseEntity<ErrorBody> response = handler.handleApi(Errors.forbidden());

            assertThat(response.getHeaders().headerNames()).containsExactly(HttpHeaders.CONTENT_TYPE);
            assertThat(response.getBody().errors()).isEmpty();
        }

        @Test
        void keepsTheStatusCodeAndMessageOfAServerErrorException() {
            ResponseEntity<ErrorBody> response = handler.handleApi(
                    new ApiException(ErrorCode.INTERNAL_ERROR, "Storage is down"));

            assertError(response, 500, "INTERNAL_ERROR", "Storage is down");
        }
    }

    @Nested
    class TraceId {

        @Test
        void putsTheTraceIdFromTheLogContextInTheBody() {
            MDC.put(MDC_KEY, TRACE_ID);

            assertThat(handler.handleApi(Errors.forbidden()).getBody().traceId()).isEqualTo(TRACE_ID);
        }

        @Test
        void leavesTheTraceIdNullWhenNoTraceFilterRan() {
            assertThat(handler.handleApi(Errors.forbidden()).getBody().traceId()).isNull();
        }

        @Test
        void includesTheTraceIdInEveryKindOfAnswer() throws Exception {
            MDC.put(MDC_KEY, TRACE_ID);
            List<Callable<ResponseEntity<ErrorBody>>> answers = List.of(
                    () -> handler.handleApi(Errors.forbidden()),
                    () -> handler.handleBinding(bindException(constraintError("name", "NotBlank", "blank"))),
                    () -> handler.handleUploadTooLarge(new MaxUploadSizeExceededException(1024)),
                    () -> handler.handleBrokenUpload(new MultipartException("boom")),
                    () -> handler.handleUnreadableBody(unreadable("bad json")),
                    () -> handler.handleTypeMismatch(new TypeMismatchException("x", Integer.class)),
                    () -> handler.handleUnexpected(new ErrorResponseException(HttpStatus.NOT_FOUND)),
                    () -> handler.handleUnexpected(new ErrorResponseException(HttpStatus.METHOD_NOT_ALLOWED)),
                    () -> handler.handleUnexpected(new IllegalStateException("boom")));

            for (Callable<ResponseEntity<ErrorBody>> answer : answers) {
                assertThat(answer.call().getBody().traceId()).isEqualTo(TRACE_ID);
            }
        }
    }

    @Nested
    class BindingErrors {

        @ParameterizedTest
        @ValueSource(strings = {"NotNull", "NotBlank", "NotEmpty"})
        void mapsPresenceConstraintsToRequired(String constraint) {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(constraintError("firstName", constraint, "must not be blank")));

            assertError(response, 422, "VALIDATION_ERROR", "Validation failed");
            assertThat(response.getBody().errors()).containsExactly(FieldErrors.required("first_name"));
        }

        @Test
        void mapsEmailToMustBeEmail() {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(constraintError("email", "Email", "must be a well-formed email address")));

            assertThat(response.getBody().errors()).containsExactly(FieldErrors.mustBeEmail("email"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Size", "Pattern", "Min"})
        void mapsAnyOtherConstraintToInvalidFormatWithItsOwnMessage(String constraint) {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(constraintError("title", constraint, "is not acceptable")));

            assertThat(response.getBody().errors())
                    .containsExactly(FieldErrors.invalidFormat("title", "is not acceptable"));
        }

        @Test
        void usesAGenericMessageWhenTheConstraintHasNone() {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(constraintError("title", "Size", null)));

            assertThat(response.getBody().errors()).containsExactly(FieldErrors.invalidFormat("title", "is invalid"));
        }

        @Test
        void treatsAFieldErrorWithoutCodesAsInvalidFormat() {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(new org.springframework.validation.FieldError("request", "title", "bad")));

            assertThat(response.getBody().errors()).containsExactly(FieldErrors.invalidFormat("title", "bad"));
        }

        @Test
        void readsTheConstraintFromTheLastCodeOfSpringsCodeArray() {
            ResponseEntity<ErrorBody> response = handler.handleBinding(bindException(
                    springFieldError("title", "too long", "NotBlank.request.title", "Size")));

            assertThat(response.getBody().errors()).containsExactly(FieldErrors.invalidFormat("title", "too long"));
        }

        @ParameterizedTest
        @CsvSource({
                "name,                name",
                "firstName,           first_name",
                "addressLine2,        address_line2",
                "line2Text,           line2_text",
                "userID,              user_id",
                "address.postalCode,  address.postal_code",
                "URLValue,            urlvalue"
        })
        void convertsFieldNamesToSnakeCase(String field, String expected) {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(constraintError(field, "NotBlank", "blank")));

            assertThat(response.getBody().errors()).containsExactly(FieldErrors.required(expected));
        }

        @Test
        void keepsSeveralErrorsInOrder() {
            ResponseEntity<ErrorBody> response = handler.handleBinding(bindException(
                    constraintError("email", "NotBlank", "blank"),
                    constraintError("password", "Size", "too short"),
                    constraintError("firstName", "NotNull", "null")));

            assertThat(response.getBody().errors()).containsExactly(
                    FieldErrors.required("email"),
                    FieldErrors.invalidFormat("password", "too short"),
                    FieldErrors.required("first_name"));
        }

        @Test
        void ignoresObjectLevelErrorsAndAnswersWithAnEmptyErrorList() {
            ResponseEntity<ErrorBody> response = handler.handleBinding(
                    bindException(new ObjectError("request", "passwords must match")));

            assertError(response, 422, "VALIDATION_ERROR", "Validation failed");
            assertThat(response.getBody().errors()).isEmpty();
        }
    }

    @Nested
    class BodyAndUploadErrors {

        @Test
        void answersAnOversizedUploadWith413() {
            ResponseEntity<ErrorBody> response = handler.handleUploadTooLarge(new MaxUploadSizeExceededException(1024));

            assertError(response, 413, "FILE_TOO_LARGE", "The file exceeds the allowed size");
        }

        @Test
        void answersABrokenUploadWithAFieldErrorAndHidesTheInternalMessage() {
            ResponseEntity<ErrorBody> response = handler.handleBrokenUpload(new MultipartException("tmp dir is full"));

            assertError(response, 422, "VALIDATION_ERROR", "Validation failed");
            assertThat(response.getBody().errors())
                    .containsExactly(FieldErrors.invalidFormat("file", "the file could not be read"));
            assertThat(String.valueOf(response.getBody())).doesNotContain("tmp dir");
        }

        @Test
        void saysTheBodyIsRequiredWhenSpringReportsItMissing() {
            ResponseEntity<ErrorBody> response = handler.handleUnreadableBody(
                    unreadable("Required request body is missing: public void create(Item)"));

            assertError(response, 400, "MALFORMED_JSON", "Request body is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {"JSON parse error: Unexpected character", "Cannot deserialize value of type int",
                "xRequired request body is missing"})
        void saysTheBodyIsNotValidJsonForEveryOtherReason(String springMessage) {
            ResponseEntity<ErrorBody> response = handler.handleUnreadableBody(unreadable(springMessage));

            assertError(response, 400, "MALFORMED_JSON", "Request body is not valid JSON");
        }

        @Test
        void saysTheBodyIsNotValidJsonWhenSpringGivesNoMessage() {
            ResponseEntity<ErrorBody> response = handler.handleUnreadableBody(unreadable(null));

            assertError(response, 400, "MALFORMED_JSON", "Request body is not valid JSON");
        }

        @Test
        void namesTheArgumentOfAMethodArgumentTypeMismatch() {
            MethodArgumentTypeMismatchException mismatch = new MethodArgumentTypeMismatchException(
                    "abc", UUID.class, "specialistId", mock(MethodParameter.class), null);

            ResponseEntity<ErrorBody> response = handler.handleTypeMismatch(mismatch);

            assertError(response, 400, "BAD_REQUEST", "Invalid value for 'specialistId'");
        }

        @Test
        void namesThePropertyOfAPlainTypeMismatch() {
            TypeMismatchException mismatch = new TypeMismatchException(
                    new PropertyChangeEvent(new Object(), "age", "1", "x"), Integer.class);

            ResponseEntity<ErrorBody> response = handler.handleTypeMismatch(mismatch);

            assertError(response, 400, "BAD_REQUEST", "Invalid value for 'age'");
        }

        @Test
        void usesTheDefaultMessageWhenTheTypeMismatchHasNoName() {
            ResponseEntity<ErrorBody> response = handler.handleTypeMismatch(new TypeMismatchException("x", Integer.class));

            assertError(response, 400, "BAD_REQUEST", "Bad request");
        }
    }

    @Nested
    class Fallback {

        @Test
        void rethrowsAuthenticationExceptionsSoSpringSecurityAnswers401() {
            BadCredentialsException exception = new BadCredentialsException("bad");

            assertThat(catchThrowable(() -> handler.handleUnexpected(exception))).isSameAs(exception);
        }

        @Test
        void rethrowsAccessDeniedExceptionsSoSpringSecurityAnswers403() {
            AccessDeniedException exception = new AccessDeniedException("denied");

            assertThat(catchThrowable(() -> handler.handleUnexpected(exception))).isSameAs(exception);
        }

        @Test
        void answersAFrameworkNotFoundWithTheStandardNotFoundBody() throws Exception {
            ResponseEntity<ErrorBody> response = handler.handleUnexpected(new ErrorResponseException(HttpStatus.NOT_FOUND));

            assertError(response, 404, "NOT_FOUND", "Resource not found");
        }

        @ParameterizedTest
        @CsvSource({
                "400, BAD_REQUEST,           Bad Request",
                "405, METHOD_NOT_ALLOWED,    Method Not Allowed",
                "406, NOT_ACCEPTABLE,        Not Acceptable",
                "415, UNSUPPORTED_MEDIA_TYPE, Unsupported Media Type",
                "503, SERVICE_UNAVAILABLE,   Service Unavailable"
        })
        void keepsTheStatusOfOtherFrameworkExceptionsAndNamesTheCodeAfterIt(int status, String code, String message)
                throws Exception {
            ResponseEntity<ErrorBody> response = handler.handleUnexpected(
                    new ErrorResponseException(HttpStatusCode.valueOf(status)));

            assertError(response, status, code, message);
        }

        @Test
        void keepsTheFrameworksOwnHeadersAndStillAnswersWithJson() throws Exception {
            ResponseEntity<ErrorBody> response = handler.handleUnexpected(
                    new HttpRequestMethodNotSupportedException("POST", List.of("GET", "PUT")));

            assertError(response, 405, "METHOD_NOT_ALLOWED", "Method Not Allowed");
            assertThat(response.getHeaders().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.PUT);
        }

        @Test
        void usesAGenericCodeAndMessageForAStatusSpringDoesNotKnow() throws Exception {
            ResponseEntity<ErrorBody> response = handler.handleUnexpected(
                    new ErrorResponseException(HttpStatusCode.valueOf(499)));

            assertError(response, 499, "HTTP_ERROR", "Request failed");
        }

        @ParameterizedTest
        @ValueSource(strings = {"jdbc:postgresql://db:5432/nemouw password=secret", "", "null pointer in line 42"})
        void answersAnUnexpectedExceptionWith500AndNeverShowsItsMessage(String internalMessage) throws Exception {
            ResponseEntity<ErrorBody> response = handler.handleUnexpected(new IllegalStateException(internalMessage));

            assertError(response, 500, "INTERNAL_ERROR", "Something went wrong");
            assertThat(String.valueOf(response.getBody())).doesNotContain("password").doesNotContain("line 42");
        }

        @Test
        void answersAnUncheckedAndACheckedExceptionTheSameWay() throws Exception {
            assertError(handler.handleUnexpected(new NullPointerException()), 500, "INTERNAL_ERROR",
                    "Something went wrong");
            assertError(handler.handleUnexpected(new java.io.IOException("disk")), 500, "INTERNAL_ERROR",
                    "Something went wrong");
        }
    }
}