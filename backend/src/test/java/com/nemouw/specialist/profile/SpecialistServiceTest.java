package com.nemouw.specialist.profile;

import com.nemouw.common.enums.Currency;
import com.nemouw.common.enums.Gender;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.*;
import com.nemouw.media.api.PublicMediaUrls;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistOnboardingRequest;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistResponse;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;
import static com.nemouw.specialist.profile.SpecialistRows.*;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpecialistServiceTest {

    private static final FieldError AGE_ORDER = FieldErrors.invalidFormat("min_age_treated",
            "min_age_treated must be less than or equal to max_age_treated");
    private static final FieldError UNKNOWN_SYNDICATE = FieldErrors.invalidFormat("syndicate_id", "unknown syndicate");
    private static final FieldError RESERVED = FieldErrors.invalidFormat("username", "is not available");

    private final SpecialistRepository specialists = mock(SpecialistRepository.class);
    private final PublicMediaUrls mediaUrls = mock(PublicMediaUrls.class);
    private final SpecialistService service = new SpecialistService(specialists, mediaUrls);

    SpecialistServiceTest() {
        when(mediaUrls.urlFor(any())).thenAnswer(call -> {
            String key = call.getArgument(0);
            return key == null ? null : "https://cdn.test/" + key;
        });
    }

    private static List<FieldError> errorsOf(ThrowingCallable call) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    private static Map<String, Object> body(Object... keysAndValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) body.put((String) keysAndValues[i], keysAndValues[i + 1]);
        return body;
    }

    private static DuplicateKeyException duplicate(String constraint) {
        return new DuplicateKeyException("duplicate",
                new SQLException("duplicate key value violates unique constraint \"" + constraint + "\""));
    }

    private static DataIntegrityViolationException integrity(String message) {
        return new DataIntegrityViolationException("integrity", new SQLException(message));
    }

    private void givenCurrent(SpecialistRow row) {
        when(specialists.findByUserId(USER_ID)).thenReturn(Optional.of(row));
        when(specialists.findById(row.id())).thenReturn(Optional.of(row));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sentChanges() {
        ArgumentCaptor<Map<String, Object>> captor =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(specialists).update(eq(SPECIALIST_ID), captor.capture());
        return captor.getValue();
    }

    private void assertNoUniquenessChecks() {
        verify(specialists, never()).usernameTaken(any(), any());
        verify(specialists, never()).memberNumberTaken(any(), any(), any());
        verify(specialists, never()).syndicateExists(any());
    }

    private void assertNothingWritten() {
        verify(specialists, never()).update(any(), any());
        verify(specialists, never()).insert(any(), any(), any(), any(), any());
    }

    private static SpecialistOnboardingRequest request(String username, UUID syndicateId, String memberNumber) {
        return new SpecialistOnboardingRequest(username, syndicateId, memberNumber);
    }

    private void givenOnboardingCanSucceed() {
        when(specialists.syndicateExists(SYNDICATE_ID)).thenReturn(true);
        when(specialists.insert(any(), any(), any(), any(), any())).thenReturn(SPECIALIST_ID);
        when(specialists.findById(SPECIALIST_ID)).thenReturn(Optional.of(pending()));
    }

    static Stream<Arguments> invalidNumbers() {
        return Stream.of("years_of_experience", "min_age_treated", "max_age_treated").flatMap(key -> Stream.of(
                arguments(key, -1, "must be between 0 and 100"),
                arguments(key, 101, "must be between 0 and 100"),
                arguments(key, 5.5, "must be an integer"),
                arguments(key, "5", "must be an integer"),
                arguments(key, 3_000_000_000L, "must be an integer")));
    }

    @Nested
    class GetMine {

        @Test
        void mapsEveryColumnToItsOwnField() {
            OffsetDateTime reviewedAt = OffsetDateTime.parse("2026-03-01T10:00:00Z");
            OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T08:00:00Z");
            OffsetDateTime updatedAt = OffsetDateTime.parse("2026-02-01T09:30:00Z");
            UUID specialtyId = UUID.fromString("e2f5a8c1-7d34-4b60-9a1e-3c8d5f0b7a42");
            SpecialistRow row = new SpecialistRow(SPECIALIST_ID, USER_ID, specialtyId, SYNDICATE_ID, "M-1001",
                    "dr.amira", "female", "+96170111222", "ar", 12, UUID.randomUUID(), "pictures/p.webp",
                    UUID.randomUUID(), "covers/c.webp", 5, 17, "rejected", reviewedAt, "Licence not found",
                    "suspended", new BigDecimal("40.50"), new BigDecimal("80.00"), "USD", createdAt, updatedAt);
            when(specialists.findByUserId(USER_ID)).thenReturn(Optional.of(row));

            SpecialistResponse response = service.getMine(USER_ID);

            assertThat(response.id()).isEqualTo(SPECIALIST_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.specialtyId()).isEqualTo(specialtyId);
            assertThat(response.syndicateId()).isEqualTo(SYNDICATE_ID);
            assertThat(response.syndicateMemberNumber()).isEqualTo("M-1001");
            assertThat(response.username()).isEqualTo("dr.amira");
            assertThat(response.gender()).isEqualTo(Gender.FEMALE);
            assertThat(response.whatsappNumber()).isEqualTo("+96170111222");
            assertThat(response.primaryLanguage()).isEqualTo(LocaleCode.AR);
            assertThat(response.yearsOfExperience()).isEqualTo(12);
            assertThat(response.profilePictureUrl()).isEqualTo("https://cdn.test/pictures/p.webp");
            assertThat(response.coverImageUrl()).isEqualTo("https://cdn.test/covers/c.webp");
            assertThat(response.minAgeTreated()).isEqualTo(5);
            assertThat(response.maxAgeTreated()).isEqualTo(17);
            assertThat(response.verificationStatus()).isEqualTo(SpecialistVerificationStatus.REJECTED);
            assertThat(response.reviewedAt()).isEqualTo(reviewedAt.toInstant());
            assertThat(response.rejectionReason()).isEqualTo("Licence not found");
            assertThat(response.userStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(response.sessionPriceMin()).isEqualByComparingTo("40.50");
            assertThat(response.sessionPriceMax()).isEqualByComparingTo("80.00");
            assertThat(response.priceCurrency()).isEqualTo(Currency.USD);
            assertThat(response.createdAt()).isEqualTo(createdAt.toInstant());
            assertThat(response.updatedAt()).isEqualTo(updatedAt.toInstant());
        }

        @Test
        void mapsMissingOptionalValuesToNull() {
            when(specialists.findByUserId(USER_ID)).thenReturn(Optional.of(pending()));

            SpecialistResponse response = service.getMine(USER_ID);

            assertThat(response.gender()).isNull();
            assertThat(response.primaryLanguage()).isNull();
            assertThat(response.profilePictureUrl()).isNull();
            assertThat(response.coverImageUrl()).isNull();
            assertThat(response.reviewedAt()).isNull();
            assertThat(response.rejectionReason()).isNull();
            assertThat(response.sessionPriceMin()).isNull();
        }

        @Test
        void answersNotFoundBeforeOnboarding() {
            assertApiError(() -> service.getMine(USER_ID), ErrorCode.SPECIALIST_NOT_FOUND);
        }
    }

    @Nested
    class Onboarding {

        @Test
        void createsTheProfileWithNormalisedValuesAndTheCountrysCurrency() {
            givenOnboardingCanSucceed();
            when(specialists.countryCurrencyCode(USER_ID)).thenReturn(Optional.of("USD"));

            SpecialistResponse response = service.onboard(USER_ID, request(" Dr.Amira ", SYNDICATE_ID, "  M-1001 "));

            verify(specialists).insert(USER_ID, "dr.amira", SYNDICATE_ID, "M-1001", Currency.USD);
            assertThat(response.id()).isEqualTo(SPECIALIST_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LBP", "EUR"})
        void fallsBackToTheDefaultCurrencyWhenTheCountrysIsNotSupported(String countryCurrency) {
            givenOnboardingCanSucceed();
            when(specialists.countryCurrencyCode(USER_ID)).thenReturn(Optional.of(countryCurrency));

            service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1001"));

            verify(specialists).insert(USER_ID, "dr.amira", SYNDICATE_ID, "M-1001", Currency.DEFAULT);
        }

        @Test
        void fallsBackToTheDefaultCurrencyWhenTheUserHasNoCountry() {
            givenOnboardingCanSucceed();

            service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1001"));

            verify(specialists).insert(USER_ID, "dr.amira", SYNDICATE_ID, "M-1001", Currency.DEFAULT);
        }

        @Test
        void acceptsTheShortestAndLongestUsername() {
            givenOnboardingCanSucceed();

            assertThatCode(() -> service.onboard(USER_ID, request("abc", SYNDICATE_ID, "M-1"))).doesNotThrowAnyException();
            assertThatCode(() -> service.onboard(USER_ID, request("a".repeat(30), SYNDICATE_ID, "M-1")))
                    .doesNotThrowAnyException();
        }

        @Test
        void acceptsAMemberNumberOfExactlyTheMaximumLength() {
            givenOnboardingCanSucceed();

            assertThatCode(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "m".repeat(TEXT_MAX))))
                    .doesNotThrowAnyException();
        }

        @Test
        void reportsEveryMissingFieldTogetherAndWritesNothing() {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request(null, null, null))))
                    .containsExactlyInAnyOrder(FieldErrors.required("username"), FieldErrors.required("syndicate_id"),
                            FieldErrors.required("syndicate_member_number"));

            verifyNoInteractions(specialists);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void requiresAUsername(String blank) {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request(blank, SYNDICATE_ID, "M-1"))))
                    .containsExactly(FieldErrors.required("username"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void requiresAMemberNumber(String blank) {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, blank))))
                    .containsExactly(FieldErrors.required("syndicate_member_number"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab", "has space", "bad!char", "\u00E9mile"})
        void rejectsAUsernameInTheWrongFormat(String username) {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request(username, SYNDICATE_ID, "M-1"))))
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("username", FieldErrorCode.INVALID_FORMAT));
        }

        @Test
        void rejectsAUsernameOfThirtyOneCharacters() {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request("a".repeat(31), SYNDICATE_ID, "M-1"))))
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("username", FieldErrorCode.INVALID_FORMAT));
        }

        @ParameterizedTest
        @ValueSource(strings = {"by-id", "nemouw", "dr.nemouw"})
        void rejectsAReservedUsername(String username) {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request(username, SYNDICATE_ID, "M-1"))))
                    .containsExactly(RESERVED);
        }

        @Test
        void rejectsAMemberNumberOneCharacterOverTheMaximum() {
            assertThat(errorsOf(() -> service.onboard(USER_ID,
                    request("dr.amira", SYNDICATE_ID, "m".repeat(TEXT_MAX + 1)))))
                    .containsExactly(FieldErrors.tooLong("syndicate_member_number", TEXT_MAX));
        }
    }

    @Nested
    class OnboardingConflicts {

        @Test
        void refusesASecondProfileForTheSameAccount() {
            when(specialists.findByUserId(USER_ID)).thenReturn(Optional.of(pending()));

            assertApiError(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1")),
                    ErrorCode.PROFILE_ALREADY_EXISTS);
            assertNothingWritten();
        }

        @Test
        void reportsInvalidInputBeforeTheExistingProfile() {
            when(specialists.findByUserId(USER_ID)).thenReturn(Optional.of(pending()));

            assertThat(errorsOf(() -> service.onboard(USER_ID, request("ab", SYNDICATE_ID, "M-1")))).isNotEmpty();
        }

        @Test
        void rejectsAnUnknownSyndicate() {
            assertThat(errorsOf(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1"))))
                    .containsExactly(UNKNOWN_SYNDICATE);
            assertNothingWritten();
        }

        @Test
        void refusesATakenUsernameUsingTheNormalisedName() {
            when(specialists.syndicateExists(SYNDICATE_ID)).thenReturn(true);
            when(specialists.usernameTaken(eq("dr.amira"), isNull())).thenReturn(true);

            assertApiError(() -> service.onboard(USER_ID, request(" Dr.Amira ", SYNDICATE_ID, "M-1")),
                    ErrorCode.USERNAME_TAKEN);
            assertNothingWritten();
        }

        @Test
        void refusesAMemberNumberAlreadyUsedInThatSyndicate() {
            when(specialists.syndicateExists(SYNDICATE_ID)).thenReturn(true);
            when(specialists.memberNumberTaken(eq(SYNDICATE_ID), eq("M-1001"), isNull())).thenReturn(true);

            assertApiError(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, " M-1001 ")),
                    ErrorCode.MEMBER_NUMBER_TAKEN);
            assertNothingWritten();
        }

        @Test
        void checksTheUsernameBeforeTheMemberNumber() {
            when(specialists.syndicateExists(SYNDICATE_ID)).thenReturn(true);
            when(specialists.usernameTaken(any(), any())).thenReturn(true);
            when(specialists.memberNumberTaken(any(), any(), any())).thenReturn(true);

            assertApiError(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1")),
                    ErrorCode.USERNAME_TAKEN);
            verify(specialists, never()).memberNumberTaken(any(), any(), any());
        }

        @ParameterizedTest
        @CsvSource({
                "uq_specialists_syndicate_member,       MEMBER_NUMBER_TAKEN",
                "specialists_user_id_key,               PROFILE_ALREADY_EXISTS",
                "specialists_username_key,              USERNAME_TAKEN",
                "uq_user_identifiers_type_identifier,   USERNAME_TAKEN"
        })
        void mapsADuplicateKeyRaceOnInsertToTheContractError(String constraint, ErrorCode expected) {
            givenOnboardingCanSucceed();
            when(specialists.insert(any(), any(), any(), any(), any())).thenThrow(duplicate(constraint));

            assertApiError(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1")), expected);
        }

        @Test
        void rethrowsAnUnknownDuplicateKeyUnchanged() {
            givenOnboardingCanSucceed();
            DuplicateKeyException unknown = duplicate("some_other_index");
            when(specialists.insert(any(), any(), any(), any(), any())).thenThrow(unknown);

            assertThat(catchThrowable(() -> service.onboard(USER_ID, request("dr.amira", SYNDICATE_ID, "M-1"))))
                    .isSameAs(unknown);
        }
    }

    @Nested
    class UpdateChanges {

        @Test
        void answersOnboardingRequiredWhenThereIsNoProfile() {
            assertApiError(() -> service.update(USER_ID, Map.of()), ErrorCode.ONBOARDING_REQUIRED);
        }

        @Test
        void changesNothingForAnEmptyBody() {
            givenCurrent(pending());

            SpecialistResponse response = service.update(USER_ID, Map.of());

            assertThat(response.id()).isEqualTo(SPECIALIST_ID);
            assertThat(sentChanges()).isEmpty();
            assertNoUniquenessChecks();
        }

        @Test
        void appliesOnlyTheKeysThatWereSent() {
            givenCurrent(pending());

            service.update(USER_ID, body("whatsapp_number", "+96170111222", "gender", "female",
                    "primary_language", "ar", "years_of_experience", 12, "min_age_treated", 3, "max_age_treated", 10));

            assertThat(sentChanges())
                    .containsOnlyKeys("whatsapp_number", "gender", "primary_language", "years_of_experience",
                            "min_age_treated", "max_age_treated")
                    .containsEntry("whatsapp_number", "+96170111222")
                    .containsEntry("gender", "female")
                    .containsEntry("primary_language", "ar")
                    .containsEntry("years_of_experience", 12)
                    .containsEntry("min_age_treated", 3)
                    .containsEntry("max_age_treated", 10);
        }

        @Test
        void clearsEveryOptionalFieldSentAsNull() {
            givenCurrent(pending());

            service.update(USER_ID, body("whatsapp_number", null, "gender", null, "primary_language", null,
                    "years_of_experience", null, "min_age_treated", null, "max_age_treated", null,
                    "session_price_min", null, "session_price_max", null));

            assertThat(sentChanges()).containsOnlyKeys("whatsapp_number", "gender", "primary_language",
                    "years_of_experience", "min_age_treated", "max_age_treated", "session_price_min",
                    "session_price_max");
            assertThat(sentChanges().values()).containsOnlyNulls();
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "\t"})
        void clearsAWhatsappNumberThatIsBlank(String blank) {
            givenCurrent(pending());

            service.update(USER_ID, body("whatsapp_number", blank));

            assertThat(sentChanges()).containsOnlyKeys("whatsapp_number").containsEntry("whatsapp_number", null);
        }

        @Test
        void storesTheWhatsappNumberStripped() {
            givenCurrent(pending());

            service.update(USER_ID, body("whatsapp_number", " \u00A0+96170111222 "));

            assertThat(sentChanges()).containsEntry("whatsapp_number", "+96170111222");
        }

        @Test
        void ignoresKeysItDoesNotKnow() {
            givenCurrent(pending());

            service.update(USER_ID, body("verification_status", "active", "user_status", "suspended",
                    "user_id", UUID.randomUUID().toString(), "specialty_id", UUID.randomUUID().toString(),
                    "id", UUID.randomUUID().toString(), "reviewed_at", "2026-01-01T00:00:00Z",
                    "rejection_reason", "x", "created_at", "2020-01-01T00:00:00Z", "unknown", 1));

            assertThat(sentChanges()).isEmpty();
        }

        @Test
        void passesTheSessionPriceThrough() {
            givenCurrent(pending());

            service.update(USER_ID, body("session_price_min", 40, "session_price_max", 80.5, "price_currency", "USD"));

            assertThat(sentChanges())
                    .containsOnlyKeys("session_price_min", "session_price_max", "price_currency")
                    .containsEntry("session_price_min", new BigDecimal("40"))
                    .containsEntry("session_price_max", new BigDecimal("80.5"))
                    .containsEntry("price_currency", "USD");
        }

        @Test
        void changesTheUsernameNormalisedAfterCheckingItIsFree() {
            givenCurrent(pending());

            service.update(USER_ID, body("username", " New.Name "));

            verify(specialists).usernameTaken("new.name", SPECIALIST_ID);
            assertThat(sentChanges()).containsOnlyKeys("username").containsEntry("username", "new.name");
        }

        @Test
        void doesNotCheckTheUsernameWhenOnlyItsCaseChanges() {
            givenCurrent(pending());

            service.update(USER_ID, body("username", "DR.AMIRA"));

            assertThat(sentChanges()).isEmpty();
            verify(specialists, never()).usernameTaken(any(), any());
        }

        @Test
        void changesTheMemberNumberAfterCheckingItIsFreeInTheSameSyndicate() {
            givenCurrent(pending());

            service.update(USER_ID, body("syndicate_member_number", " M-2002 "));

            verify(specialists).memberNumberTaken(SYNDICATE_ID, "M-2002", SPECIALIST_ID);
            assertThat(sentChanges()).containsOnlyKeys("syndicate_member_number")
                    .containsEntry("syndicate_member_number", "M-2002");
        }

        @Test
        void doesNotCheckAMemberNumberThatDidNotChange() {
            givenCurrent(pending());

            service.update(USER_ID, body("syndicate_member_number", " M-1001 "));

            assertThat(sentChanges()).isEmpty();
            verify(specialists, never()).memberNumberTaken(any(), any(), any());
        }

        @ParameterizedTest
        @EnumSource(value = SpecialistVerificationStatus.class, names = {"PENDING", "REJECTED"})
        void changesTheMemberNumberOfAnUnverifiedSpecialist(SpecialistVerificationStatus status) {
            givenCurrent(row(status));

            service.update(USER_ID, body("syndicate_member_number", "M-2002"));

            assertThat(sentChanges()).containsEntry("syndicate_member_number", "M-2002");
        }

        @ParameterizedTest
        @EnumSource(value = SpecialistVerificationStatus.class, names = {"PENDING", "REJECTED"})
        void movesAnUnverifiedSpecialistToAnotherSyndicateWithANewMemberNumber(SpecialistVerificationStatus status) {
            givenCurrent(row(status));
            when(specialists.syndicateExists(OTHER_SYNDICATE_ID)).thenReturn(true);

            service.update(USER_ID, body("syndicate_id", OTHER_SYNDICATE_ID.toString(), "syndicate_member_number", "M-77"));

            verify(specialists).memberNumberTaken(OTHER_SYNDICATE_ID, "M-77", SPECIALIST_ID);
            assertThat(sentChanges()).containsOnlyKeys("syndicate_id", "syndicate_member_number")
                    .containsEntry("syndicate_id", OTHER_SYNDICATE_ID)
                    .containsEntry("syndicate_member_number", "M-77");
        }

        @Test
        void treatsTheCurrentSyndicateSentAgainAsNoChange() {
            givenCurrent(pending());

            service.update(USER_ID, body("syndicate_id", " " + SYNDICATE_ID.toString().toUpperCase() + " "));

            assertThat(sentChanges()).isEmpty();
            assertNoUniquenessChecks();
        }

        @Test
        void letsAVerifiedSpecialistChangeEverythingExceptTheSyndicateAndMemberNumber() {
            givenCurrent(row(SpecialistVerificationStatus.ACTIVE));

            service.update(USER_ID, body("whatsapp_number", "+96170111222", "username", "new.name"));

            assertThat(sentChanges()).containsEntry("whatsapp_number", "+96170111222")
                    .containsEntry("username", "new.name");
        }
    }

    @Nested
    class UpdateValidation {

        private void assertInvalid(Map<String, Object> body, FieldError... expected) {
            givenCurrent(pending());
            assertThat(errorsOf(() -> service.update(USER_ID, body))).containsExactly(expected);
            assertNothingWritten();
        }

        @ParameterizedTest
        @ValueSource(strings = {"username", "syndicate_member_number", "syndicate_id"})
        void cannotClearAMandatoryFieldWithNull(String key) {
            assertInvalid(body(key, null), FieldErrors.required(key));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void cannotBlankAMandatoryTextField(String blank) {
            assertInvalid(body("username", blank), FieldErrors.required("username"));
            assertInvalid(body("syndicate_member_number", blank), FieldErrors.required("syndicate_member_number"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"username", "syndicate_member_number", "whatsapp_number"})
        void rejectsATextFieldThatIsNotAString(String key) {
            assertInvalid(body(key, 123), FieldErrors.invalidFormat(key, "must be a string"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab", "has space", "bad!char", "\u00E9mile", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
        void rejectsAUsernameInTheWrongFormat(String username) {
            givenCurrent(pending());

            assertThat(errorsOf(() -> service.update(USER_ID, body("username", username))))
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("username", FieldErrorCode.INVALID_FORMAT));
        }

        @ParameterizedTest
        @ValueSource(strings = {"by-id", "nemouw"})
        void rejectsAReservedUsername(String username) {
            assertInvalid(body("username", username), RESERVED);
        }

        @Test
        void rejectsAMemberNumberOneCharacterOverTheMaximum() {
            assertInvalid(body("syndicate_member_number", "m".repeat(TEXT_MAX + 1)),
                    FieldErrors.tooLong("syndicate_member_number", TEXT_MAX));
        }

        @Test
        void acceptsAMemberNumberOfExactlyTheMaximumLength() {
            givenCurrent(pending());

            service.update(USER_ID, body("syndicate_member_number", "m".repeat(TEXT_MAX)));

            assertThat(sentChanges()).containsEntry("syndicate_member_number", "m".repeat(TEXT_MAX));
        }

        @Test
        void rejectsAWhatsappNumberOneCharacterOverTheMaximum() {
            assertInvalid(body("whatsapp_number", "1".repeat(TEXT_MAX + 1)),
                    FieldErrors.tooLong("whatsapp_number", TEXT_MAX));
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-uuid", ""})
        void rejectsASyndicateIdThatIsNotAUuid(String value) {
            assertInvalid(body("syndicate_id", value), FieldErrors.invalidFormat("syndicate_id", "must be a valid UUID"));
        }

        @Test
        void rejectsASyndicateIdThatIsNotAString() {
            assertInvalid(body("syndicate_id", 123), FieldErrors.invalidFormat("syndicate_id", "must be a valid UUID"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"other", ""})
        void rejectsAGenderOutsideTheCodes(String gender) {
            assertInvalid(body("gender", gender), FieldErrors.invalidFormat("gender", "must be one of male, female"));
        }

        @Test
        void rejectsAGenderThatIsNotAString() {
            assertInvalid(body("gender", 5), FieldErrors.invalidFormat("gender", "must be one of male, female"));
        }

        @Test
        void rejectsALanguageOutsideTheCodes() {
            assertInvalid(body("primary_language", "de"),
                    FieldErrors.invalidFormat("primary_language", "must be one of en, ar, fr"));
        }

        @ParameterizedTest
        @MethodSource("com.nemouw.specialist.profile.SpecialistServiceTest#invalidNumbers")
        void rejectsANumberThatIsOutOfRangeOrNotAnInteger(String key, Object value, String message) {
            givenCurrent(pending());

            assertThat(errorsOf(() -> service.update(USER_ID, body(key, value))))
                    .contains(FieldErrors.invalidFormat(key, message));
            assertNothingWritten();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 100})
        void acceptsYearsOfExperienceFromZeroToOneHundred(int years) {
            givenCurrent(pending());

            service.update(USER_ID, body("years_of_experience", years));

            assertThat(sentChanges()).containsEntry("years_of_experience", years);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 100})
        void acceptsAgesFromZeroToOneHundred(int age) {
            givenCurrent(pending());

            service.update(USER_ID, body("min_age_treated", age, "max_age_treated", age));

            assertThat(sentChanges()).containsEntry("min_age_treated", age).containsEntry("max_age_treated", age);
        }

        @Test
        void rejectsAMaximumAgeBelowTheStoredMinimum() {
            assertInvalid(body("max_age_treated", 4), AGE_ORDER);
        }

        @Test
        void rejectsAMinimumAgeAboveTheStoredMaximum() {
            assertInvalid(body("min_age_treated", 18), AGE_ORDER);
        }

        @Test
        void acceptsEqualAgesAndClearingOneSide() {
            givenCurrent(pending());

            service.update(USER_ID, body("min_age_treated", 10, "max_age_treated", 10));
            assertThat(sentChanges()).containsEntry("min_age_treated", 10).containsEntry("max_age_treated", 10);
        }

        @Test
        void acceptsClearingTheMaximumAge() {
            givenCurrent(pending());

            service.update(USER_ID, body("max_age_treated", null));

            assertThat(sentChanges()).containsOnlyKeys("max_age_treated").containsEntry("max_age_treated", null);
        }

        @Test
        void rejectsAnInvalidPriceThroughTheSamePath() {
            givenCurrent(pending());

            assertThat(errorsOf(() -> service.update(USER_ID, body("session_price_min", -1))))
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("session_price_min", FieldErrorCode.INVALID_FORMAT));
        }

        @Test
        void requiresAMemberNumberWhenTheSyndicateChanges() {
            assertInvalid(body("syndicate_id", OTHER_SYNDICATE_ID.toString()),
                    FieldErrors.required("syndicate_member_number"));
            assertNoUniquenessChecks();
        }

        @Test
        void reportsEveryInvalidFieldTogetherBeforeTouchingTheDatabase() {
            givenCurrent(pending());

            assertThat(errorsOf(() -> service.update(USER_ID,
                    body("username", "ab", "gender", "x", "years_of_experience", -1))))
                    .extracting(FieldError::field)
                    .containsExactlyInAnyOrder("username", "gender", "years_of_experience");
            assertNoUniquenessChecks();
            assertNothingWritten();
        }
    }

    @Nested
    class UpdateConflictsAndLocks {

        @Test
        void refusesATakenUsername() {
            givenCurrent(pending());
            when(specialists.usernameTaken("new.name", SPECIALIST_ID)).thenReturn(true);

            assertApiError(() -> service.update(USER_ID, body("username", "new.name")), ErrorCode.USERNAME_TAKEN);
            assertNothingWritten();
        }

        @Test
        void refusesAMemberNumberTakenInTheSameSyndicate() {
            givenCurrent(pending());
            when(specialists.memberNumberTaken(SYNDICATE_ID, "M-2002", SPECIALIST_ID)).thenReturn(true);

            assertApiError(() -> service.update(USER_ID, body("syndicate_member_number", "M-2002")),
                    ErrorCode.MEMBER_NUMBER_TAKEN);
            assertNothingWritten();
        }

        @Test
        void rejectsAnUnknownNewSyndicate() {
            givenCurrent(pending());

            assertThat(errorsOf(() -> service.update(USER_ID,
                    body("syndicate_id", OTHER_SYNDICATE_ID.toString(), "syndicate_member_number", "M-77"))))
                    .containsExactly(UNKNOWN_SYNDICATE);
            assertNothingWritten();
        }

        @Test
        void refusesAMemberNumberTakenInTheNewSyndicate() {
            givenCurrent(pending());
            when(specialists.syndicateExists(OTHER_SYNDICATE_ID)).thenReturn(true);
            when(specialists.memberNumberTaken(OTHER_SYNDICATE_ID, "M-77", SPECIALIST_ID)).thenReturn(true);

            assertApiError(() -> service.update(USER_ID,
                            body("syndicate_id", OTHER_SYNDICATE_ID.toString(), "syndicate_member_number", "M-77")),
                    ErrorCode.MEMBER_NUMBER_TAKEN);
            assertNothingWritten();
        }

        @Test
        void locksTheSyndicateOfAVerifiedSpecialistBeforeLookingItUp() {
            givenCurrent(row(SpecialistVerificationStatus.ACTIVE));

            assertApiError(() -> service.update(USER_ID,
                            body("syndicate_id", OTHER_SYNDICATE_ID.toString(), "syndicate_member_number", "M-77")),
                    ErrorCode.SYNDICATE_LOCKED);
            verify(specialists, never()).syndicateExists(any());
            assertNothingWritten();
        }

        @Test
        void locksTheMemberNumberOfAVerifiedSpecialist() {
            givenCurrent(row(SpecialistVerificationStatus.ACTIVE));

            assertApiError(() -> service.update(USER_ID, body("syndicate_member_number", "M-2002")),
                    ErrorCode.MEMBER_NUMBER_LOCKED);
            verify(specialists, never()).memberNumberTaken(any(), any(), any());
            assertNothingWritten();
        }

        @Test
        void allowsAVerifiedSpecialistToSendTheSameMemberNumberAgain() {
            givenCurrent(row(SpecialistVerificationStatus.ACTIVE));

            service.update(USER_ID, body("syndicate_member_number", " M-1001 "));

            assertThat(sentChanges()).isEmpty();
        }
    }

    @Nested
    class WriteTimeRaces {

        private void givenTheWriteFails(RuntimeException failure) {
            givenCurrent(pending());
            doThrow(failure).when(specialists).update(eq(SPECIALIST_ID), any());
        }

        private ThrowingCallable anyUpdate() {
            return () -> service.update(USER_ID, body("whatsapp_number", "+96170111222"));
        }

        @ParameterizedTest
        @CsvSource({
                "uq_specialists_syndicate_member,       MEMBER_NUMBER_TAKEN",
                "specialists_user_id_key,               PROFILE_ALREADY_EXISTS",
                "specialists_username_key,              USERNAME_TAKEN",
                "uq_user_identifiers_type_identifier,   USERNAME_TAKEN"
        })
        void mapsADuplicateKeyToTheContractError(String constraint, ErrorCode expected) {
            givenTheWriteFails(duplicate(constraint));

            assertApiError(anyUpdate(), expected);
        }

        @Test
        void rethrowsAnUnknownDuplicateKeyUnchanged() {
            DuplicateKeyException unknown = duplicate("some_other_index");
            givenTheWriteFails(unknown);

            assertThat(catchThrowable(anyUpdate())).isSameAs(unknown);
        }

        @ParameterizedTest
        @CsvSource({
                "syndicate_id cannot change once verified,                SYNDICATE_LOCKED",
                "syndicate_member_number cannot change once verified,     MEMBER_NUMBER_LOCKED"
        })
        void mapsTheVerificationTriggersToTheLockedErrors(String message, ErrorCode expected) {
            givenTheWriteFails(integrity(message));

            assertApiError(anyUpdate(), expected);
        }

        @Test
        void mapsTheAgeRangeConstraintToAFieldError() {
            givenTheWriteFails(integrity("violates check constraint \"ck_specialists_age_range\""));

            assertThat(errorsOf(anyUpdate())).containsExactly(AGE_ORDER);
        }

        @Test
        void mapsThePriceRangeConstraintToAFieldError() {
            givenTheWriteFails(integrity("violates check constraint \"ck_specialists_price_range\""));

            assertThat(errorsOf(anyUpdate())).containsExactly(
                    FieldErrors.invalidFormat("session_price_max", "must be greater than or equal to session_price_min"));
        }

        @Test
        void rethrowsAnUnknownIntegrityViolationUnchanged() {
            DataIntegrityViolationException unknown = integrity("violates something else");
            givenTheWriteFails(unknown);

            assertThat(catchThrowable(anyUpdate())).isSameAs(unknown);
        }
    }
}