package com.nemouw.syndicate.management;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.*;
import com.nemouw.country.CountryLookup;
import com.nemouw.specialty.SpecialtyLookup;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateCreateRequest;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateResponse;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateSummary;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateUpdateRequest;
import com.nemouw.syndicate.staff.SyndicateOwners;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import com.nemouw.user.api.UserSummary;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyndicateServiceTest {

    private static final UUID SYNDICATE_ID = UUID.fromString("a1c4e7f0-3b52-4d96-8e1a-60f2b9d7c314");
    private static final UUID SPECIALTY_ID = UUID.fromString("e2f5a8c1-7d34-4b60-9a1e-3c8d5f0b7a42");
    private static final UUID COUNTRY_ID = UUID.fromString("c8d29b41-6f07-4a3e-b5d1-9e40a7f2c685");
    private static final UUID OWNER_USER_ID = UUID.fromString("e41b8c07-6d29-4f53-8a90-1c5d3b7e2a68");

    private static final Syndicate SAVED = new Syndicate(SYNDICATE_ID, "Lebanese Order", SPECIALTY_ID, COUNTRY_ID,
            "contact@order.org", "+9611234567", "active", OffsetDateTime.parse("2026-02-01T09:30:00Z"));
    private static final SyndicateStaffResponse OWNER = new SyndicateStaffResponse(
            UUID.fromString("3d6a1f80-92b4-4e17-a5c3-8b0e7d2f4961"), SYNDICATE_ID,
            new UserSummary(OWNER_USER_ID, "Amira", "Haddad", "owner@order.org", UserRole.SYNDICATE_STAFF,
                    UserStatus.PENDING, Instant.parse("2026-02-01T09:30:00Z")),
            null, true, Instant.parse("2026-02-01T09:30:00Z"));
    private static final Invite INVITE = new Invite(OWNER_USER_ID, "owner@order.org", null);

    private final SyndicateRepository syndicates = mock(SyndicateRepository.class);
    private final CountryLookup countryLookup = mock(CountryLookup.class);
    private final SpecialtyLookup specialtyLookup = mock(SpecialtyLookup.class);
    private final InviteService invites = mock(InviteService.class);
    private final SyndicateOwners owners = mock(SyndicateOwners.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final SyndicateService service =
            new SyndicateService(syndicates, countryLookup, specialtyLookup, invites, owners, tx);

    SyndicateServiceTest() {
        when(tx.execute(any())).thenAnswer(call ->
                ((TransactionCallback<?>) call.getArgument(0)).doInTransaction(null));
    }

    private static SyndicateCreateRequest request(String name, UUID specialtyId, UUID countryId, String contactEmail,
                                                  String contactPhone, String ownerFirstName, String ownerLastName,
                                                  String ownerEmail) {
        return new SyndicateCreateRequest(name, specialtyId, countryId, contactEmail, contactPhone, ownerFirstName,
                ownerLastName, ownerEmail);
    }

    private static SyndicateCreateRequest valid() {
        return request("Lebanese Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", null, "Amira", "Haddad",
                "owner@order.org");
    }

    private static SyndicateCreateRequest withField(String field, String value) {
        return switch (field) {
            case "name" -> request(value, SPECIALTY_ID, COUNTRY_ID, "contact@order.org", null, "Amira", "Haddad",
                    "owner@order.org");
            case "contact_phone" -> request("Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", value, "Amira",
                    "Haddad", "owner@order.org");
            case "owner_first_name" -> request("Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", null, value,
                    "Haddad", "owner@order.org");
            case "owner_last_name" -> request("Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", null, "Amira",
                    value, "owner@order.org");
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static List<FieldError> errorsOf(ThrowingCallable call) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    private void givenCreateCanSucceed() {
        when(specialtyLookup.existsById(SPECIALTY_ID)).thenReturn(true);
        when(countryLookup.existsById(COUNTRY_ID)).thenReturn(true);
        when(invites.createPendingAccount(any(), any(), any(), any())).thenReturn(INVITE);
        when(syndicates.insert(any(), any(), any(), any(), any())).thenReturn(SAVED);
        when(owners.addOwner(SYNDICATE_ID, OWNER_USER_ID)).thenReturn(OWNER);
    }

    private void assertNothingCreated() {
        verify(invites, never()).createPendingAccount(any(), any(), any(), any());
        verify(syndicates, never()).insert(any(), any(), any(), any(), any());
        verify(owners, never()).addOwner(any(), any());
        verify(invites, never()).sendInvite(any(), any());
    }

    private void givenCurrent() {
        when(syndicates.findById(SYNDICATE_ID)).thenReturn(Optional.of(SAVED));
        when(syndicates.update(any(), any(), any(), any())).thenReturn(SAVED);
    }

    @Nested
    class Create {

        @Test
        void createsTheSyndicateWithItsOwnerAndSendsTheInviteAfterTheTransaction() {
            givenCreateCanSucceed();

            SyndicateResponse response = service.create(request("  Lebanese Order  ", SPECIALTY_ID, COUNTRY_ID,
                    " Contact@Order.ORG ", "   ", " Amira ", " Haddad ", "OWNER@Order.org"));

            verify(invites).createPendingAccount(UserRole.SYNDICATE_STAFF, " Amira ", " Haddad ", "OWNER@Order.org");
            verify(syndicates).insert("Lebanese Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", null);
            verify(owners).addOwner(SYNDICATE_ID, OWNER_USER_ID);
            InOrder order = inOrder(tx, invites);
            order.verify(tx).execute(any());
            order.verify(invites).sendInvite(INVITE, LocaleCode.EN);
            assertThat(response.id()).isEqualTo(SYNDICATE_ID);
            assertThat(response.name()).isEqualTo("Lebanese Order");
            assertThat(response.contactEmail()).isEqualTo("contact@order.org");
            assertThat(response.contactPhone()).isEqualTo("+9611234567");
            assertThat(response.status()).isEqualTo(SyndicateStatus.ACTIVE);
            assertThat(response.owner()).isSameAs(OWNER);
            assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-02-01T09:30:00Z"));
        }

        @Test
        void keepsAPhoneNumberThatWasGiven() {
            givenCreateCanSucceed();

            service.create(request("Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", " +9611234567 ", "Amira",
                    "Haddad", "owner@order.org"));

            verify(syndicates).insert("Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org", "+9611234567");
        }

        @Test
        void acceptsEveryTextFieldAtExactlyTheMaximumLength() {
            givenCreateCanSucceed();

            assertThatCode(() -> service.create(request("n".repeat(TEXT_MAX), SPECIALTY_ID, COUNTRY_ID,
                    "contact@order.org", "1".repeat(TEXT_MAX), "f".repeat(TEXT_MAX), "l".repeat(TEXT_MAX),
                    "owner@order.org"))).doesNotThrowAnyException();
        }

        @Test
        void reportsEveryMissingFieldTogetherAndTouchesNothing() {
            assertThat(errorsOf(() -> service.create(request(null, null, null, null, null, null, null, null))))
                    .containsExactlyInAnyOrder(FieldErrors.required("name"), FieldErrors.required("specialty_id"),
                            FieldErrors.required("country_id"), FieldErrors.required("contact_email"),
                            FieldErrors.required("owner_first_name"), FieldErrors.required("owner_last_name"),
                            FieldErrors.required("owner_email"));

            verifyNoInteractions(syndicates, countryLookup, specialtyLookup, invites, owners, tx);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void requiresNonBlankNameAndOwnerNames(String blank) {
            for (String field : List.of("name", "owner_first_name", "owner_last_name")) {
                assertThat(errorsOf(() -> service.create(withField(field, blank))))
                        .containsExactly(FieldErrors.required(field));
            }
        }

        @Test
        void rejectsAContactEmailAndAnOwnerEmailThatAreNotEmails() {
            assertThat(errorsOf(() -> service.create(request("Order", SPECIALTY_ID, COUNTRY_ID, "not-an-email", null,
                    "Amira", "Haddad", "owner@order.org"))))
                    .containsExactly(FieldErrors.mustBeEmail("contact_email"));
            assertThat(errorsOf(() -> service.create(request("Order", SPECIALTY_ID, COUNTRY_ID, "contact@order.org",
                    null, "Amira", "Haddad", "nope"))))
                    .containsExactly(FieldErrors.mustBeEmail("owner_email"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"name", "contact_phone", "owner_first_name", "owner_last_name"})
        void rejectsAValueOneCharacterOverTheMaximum(String field) {
            assertThat(errorsOf(() -> service.create(withField(field, "x".repeat(TEXT_MAX + 1)))))
                    .containsExactly(FieldErrors.tooLong(field, TEXT_MAX));
            verifyNoInteractions(syndicates, invites, tx);
        }

        @Test
        void rejectsAnUnknownSpecialtyBeforeAnUnknownCountry() {
            assertThat(errorsOf(() -> service.create(valid())))
                    .containsExactly(FieldErrors.invalidFormat("specialty_id", "unknown specialty"));
            assertNothingCreated();
        }

        @Test
        void rejectsAnUnknownCountry() {
            when(specialtyLookup.existsById(SPECIALTY_ID)).thenReturn(true);

            assertThat(errorsOf(() -> service.create(valid())))
                    .containsExactly(FieldErrors.invalidFormat("country_id", "unknown country"));
            assertNothingCreated();
        }

        @Test
        void refusesASecondSyndicateForTheSameCountryAndSpecialty() {
            when(specialtyLookup.existsById(SPECIALTY_ID)).thenReturn(true);
            when(countryLookup.existsById(COUNTRY_ID)).thenReturn(true);
            when(syndicates.existsByCountryAndSpecialty(COUNTRY_ID, SPECIALTY_ID)).thenReturn(true);

            assertApiError(() -> service.create(valid()), ErrorCode.SYNDICATE_EXISTS);
            assertNothingCreated();
        }

        @Test
        void passesOnAnOwnerEmailThatIsAlreadyRegisteredWithoutCreatingAnything() {
            when(specialtyLookup.existsById(SPECIALTY_ID)).thenReturn(true);
            when(countryLookup.existsById(COUNTRY_ID)).thenReturn(true);
            when(invites.createPendingAccount(any(), any(), any(), any())).thenThrow(Errors.emailTaken());

            assertApiError(() -> service.create(valid()), ErrorCode.EMAIL_TAKEN);
            verify(syndicates, never()).insert(any(), any(), any(), any(), any());
            verify(invites, never()).sendInvite(any(), any());
        }

        @Test
        void mapsALostRaceOnInsertToSyndicateExistsAndSendsNoInvite() {
            givenCreateCanSucceed();
            when(syndicates.insert(any(), any(), any(), any(), any())).thenThrow(new DuplicateKeyException("dup"));

            assertApiError(() -> service.create(valid()), ErrorCode.SYNDICATE_EXISTS);
            verify(owners, never()).addOwner(any(), any());
            verify(invites, never()).sendInvite(any(), any());
        }
    }

    @Nested
    class Update {

        @Test
        void keepsEveryCurrentValueForFieldsThatAreNull() {
            givenCurrent();
            when(owners.findOwner(SYNDICATE_ID)).thenReturn(Optional.of(OWNER));

            SyndicateResponse response = service.update(SYNDICATE_ID, new SyndicateUpdateRequest(null, null, null));

            verify(syndicates).update(SYNDICATE_ID, "Lebanese Order", "contact@order.org", "+9611234567");
            assertThat(response.owner()).isSameAs(OWNER);
        }

        @Test
        void appliesTheValuesThatWereSentStrippedAndNormalised() {
            givenCurrent();

            service.update(SYNDICATE_ID, new SyndicateUpdateRequest(" \u00A0New Name ", " NEW@Order.org ", " +9612 "));

            verify(syndicates).update(SYNDICATE_ID, "New Name", "new@order.org", "+9612");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void clearsThePhoneWhenItIsSentBlank(String blank) {
            givenCurrent();

            service.update(SYNDICATE_ID, new SyndicateUpdateRequest(null, null, blank));

            verify(syndicates).update(SYNDICATE_ID, "Lebanese Order", "contact@order.org", null);
        }

        @Test
        void answersWithoutAnOwnerWhenThereIsNone() {
            givenCurrent();

            assertThat(service.update(SYNDICATE_ID, new SyndicateUpdateRequest(null, null, null)).owner()).isNull();
        }

        @Test
        void answersNotFoundForAnUnknownSyndicateAndWritesNothing() {
            assertApiError(() -> service.update(SYNDICATE_ID, new SyndicateUpdateRequest("New", null, null)),
                    ErrorCode.NOT_FOUND);
            verify(syndicates, never()).update(any(), any(), any(), any());
        }

        @Test
        void answersNotFoundBeforeLookingAtTheBody() {
            assertApiError(() -> service.update(SYNDICATE_ID, new SyndicateUpdateRequest("   ", "bad", null)),
                    ErrorCode.NOT_FOUND);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void rejectsABlankNameOrContactEmail(String blank) {
            givenCurrent();

            assertThat(errorsOf(() -> service.update(SYNDICATE_ID, new SyndicateUpdateRequest(blank, null, null))))
                    .containsExactly(FieldErrors.required("name"));
            assertThat(errorsOf(() -> service.update(SYNDICATE_ID, new SyndicateUpdateRequest(null, blank, null))))
                    .containsExactly(FieldErrors.required("contact_email"));
            verify(syndicates, never()).update(any(), any(), any(), any());
        }

        @Test
        void rejectsAContactEmailThatIsNotAnEmail() {
            givenCurrent();

            assertThat(errorsOf(() -> service.update(SYNDICATE_ID, new SyndicateUpdateRequest(null, "nope", null))))
                    .containsExactly(FieldErrors.mustBeEmail("contact_email"));
        }

        @Test
        void rejectsANameAndAPhoneOneCharacterOverTheMaximum() {
            givenCurrent();

            assertThat(errorsOf(() -> service.update(SYNDICATE_ID,
                    new SyndicateUpdateRequest("x".repeat(TEXT_MAX + 1), null, "1".repeat(TEXT_MAX + 1)))))
                    .containsExactlyInAnyOrder(FieldErrors.tooLong("name", TEXT_MAX),
                            FieldErrors.tooLong("contact_phone", TEXT_MAX));
        }
    }

    @Nested
    class ListAndGet {

        @Test
        void passesTheFiltersThroughAndReturnsOnlyThePublicSummary() {
            when(syndicates.findAll(SPECIALTY_ID, null)).thenReturn(List.of(SAVED));

            List<SyndicateSummary> summaries = service.list(SPECIALTY_ID, null);

            assertThat(summaries).singleElement().satisfies(summary -> {
                assertThat(summary.id()).isEqualTo(SYNDICATE_ID);
                assertThat(summary.name()).isEqualTo("Lebanese Order");
                assertThat(summary.specialtyId()).isEqualTo(SPECIALTY_ID);
                assertThat(summary.countryId()).isEqualTo(COUNTRY_ID);
            });
        }

        @Test
        void keepsContactAndStaffDetailOutOfTheSummary() {
            assertThat(Arrays.stream(SyndicateSummary.class.getRecordComponents()).map(RecordComponent::getName))
                    .containsExactly("id", "name", "specialtyId", "countryId");
        }

        @Test
        void returnsASingleSummary() {
            when(syndicates.findById(SYNDICATE_ID)).thenReturn(Optional.of(SAVED));

            assertThat(service.get(SYNDICATE_ID).name()).isEqualTo("Lebanese Order");
        }

        @Test
        void answersNotFoundForAnUnknownSyndicate() {
            assertApiError(() -> service.get(SYNDICATE_ID), ErrorCode.NOT_FOUND);
        }
    }
}