package com.nemouw.syndicate.staff;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.exception.*;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffEmailRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffInviteRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffUpdateRequest;
import com.nemouw.user.api.AccountService;
import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;
import static com.nemouw.support.Fixtures.assertApiError;
import static com.nemouw.syndicate.staff.StaffRows.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyndicateStaffServiceTest {

    private static final Invite INVITE = new Invite(STAFF_USER_ID, "layla@order.org", null);

    private final SyndicateStaffRepository staff = mock(SyndicateStaffRepository.class);
    private final AccountService accounts = mock(AccountService.class);
    private final InviteService invites = mock(InviteService.class);
    private final SyndicateStaffAssembler assembler = mock(SyndicateStaffAssembler.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final SyndicateStaffService service = new SyndicateStaffService(staff, accounts, invites, assembler, tx);

    SyndicateStaffServiceTest() {
        when(tx.execute(any())).thenAnswer(call ->
                ((TransactionCallback<?>) call.getArgument(0)).doInTransaction(null));
    }

    private static List<FieldError> errorsOf(ThrowingCallable call) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    private static SyndicateStaffInviteRequest invite(String first, String last, String email, String title) {
        return new SyndicateStaffInviteRequest(first, last, email, title);
    }

    private void givenMember() {
        SyndicateStaffRow member = StaffRows.member();
        when(staff.findInSyndicate(STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(member));
        when(assembler.toResponse(member)).thenReturn(StaffRows.response(member));
    }

    private void givenSyndicate() {
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
    }

    private void givenInviteCanSucceed() {
        when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
        when(invites.createPendingAccount(any(), any(), any(), any())).thenReturn(INVITE);
        when(staff.insertMember(any(), any(), any())).thenReturn(STAFF_ID);
        givenMember();
    }

    private void assertNobodyInvited() {
        verify(invites, never()).createPendingAccount(any(), any(), any(), any());
        verify(staff, never()).insertMember(any(), any(), any());
        verify(invites, never()).sendInvite(any(), any());
    }

    @Nested
    class ListAndGet {

        @Test
        void listsTheStaffOfAKnownSyndicateInOneAssemblerCall() {
            List<SyndicateStaffRow> rows = List.of(StaffRows.owner(), StaffRows.member());
            List<SyndicateStaffResponse> responses = rows.stream().map(StaffRows::response).toList();
            when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
            when(staff.findBySyndicate(SYNDICATE_ID)).thenReturn(rows);
            when(assembler.toResponses(rows)).thenReturn(responses);

            assertThat(service.list(SYNDICATE_ID)).isEqualTo(responses);
        }

        @Test
        void answersNotFoundWhenListingAnUnknownSyndicate() {
            assertApiError(() -> service.list(SYNDICATE_ID), ErrorCode.NOT_FOUND);
        }

        @Test
        void getsAStaffMemberUsingBothIds() {
            givenMember();

            SyndicateStaffResponse response = service.get(SYNDICATE_ID, STAFF_ID);

            verify(staff).findInSyndicate(STAFF_ID, SYNDICATE_ID);
            assertThat(response.id()).isEqualTo(STAFF_ID);
        }

        @Test
        void answersNotFoundForAStaffIdFromAnotherSyndicate() {
            assertApiError(() -> service.get(SYNDICATE_ID, STAFF_ID), ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class Invite_ {

        @Test
        void createsAPendingStaffAccountAndSendsTheInviteAfterTheTransaction() {
            givenInviteCanSucceed();

            SyndicateStaffResponse response = service.invite(SYNDICATE_ID,
                    invite(" Layla ", " Khoury ", "LAYLA@Order.org", "  Registrar  "));

            verify(invites).createPendingAccount(UserRole.SYNDICATE_STAFF, " Layla ", " Khoury ", "LAYLA@Order.org");
            verify(staff).insertMember(SYNDICATE_ID, STAFF_USER_ID, "Registrar");
            InOrder order = inOrder(tx, invites);
            order.verify(tx).execute(any());
            order.verify(invites).sendInvite(INVITE, LocaleCode.EN);
            assertThat(response.id()).isEqualTo(STAFF_ID);
            assertThat(response.owner()).isFalse();
        }

        @Test
        void acceptsEveryTextFieldAtExactlyTheMaximumLength() {
            givenInviteCanSucceed();

            service.invite(SYNDICATE_ID, invite("f".repeat(TEXT_MAX), "l".repeat(TEXT_MAX), "layla@order.org",
                    "t".repeat(TEXT_MAX)));

            verify(staff).insertMember(SYNDICATE_ID, STAFF_USER_ID, "t".repeat(TEXT_MAX));
        }

        @Test
        void reportsEveryMissingFieldTogetherAndTouchesNothing() {
            givenSyndicate();

            assertThat(errorsOf(() -> service.invite(SYNDICATE_ID, invite(null, null, null, null))))
                    .containsExactlyInAnyOrder(FieldErrors.required("first_name"), FieldErrors.required("last_name"),
                            FieldErrors.required("email"), FieldErrors.required("title"));

            assertNobodyInvited();
            verifyNoInteractions(tx);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void requiresNonBlankNamesAndTitle(String blank) {
            givenSyndicate();

            assertThat(errorsOf(() -> service.invite(SYNDICATE_ID, invite(blank, "Khoury", "a@b.org", "Registrar"))))
                    .containsExactly(FieldErrors.required("first_name"));
            assertThat(errorsOf(() -> service.invite(SYNDICATE_ID, invite("Layla", blank, "a@b.org", "Registrar"))))
                    .containsExactly(FieldErrors.required("last_name"));
            assertThat(errorsOf(() -> service.invite(SYNDICATE_ID, invite("Layla", "Khoury", "a@b.org", blank))))
                    .containsExactly(FieldErrors.required("title"));
        }

        @Test
        void rejectsAnEmailThatIsNotAnEmail() {
            givenSyndicate();

            assertThat(errorsOf(() -> service.invite(SYNDICATE_ID, invite("Layla", "Khoury", "nope", "Registrar"))))
                    .containsExactly(FieldErrors.mustBeEmail("email"));
        }

        @Test
        void rejectsNamesAndTitleOneCharacterOverTheMaximum() {
            givenSyndicate();
            String tooLong = "x".repeat(TEXT_MAX + 1);

            assertThat(errorsOf(() -> service.invite(SYNDICATE_ID, invite(tooLong, tooLong, "a@b.org", tooLong))))
                    .containsExactlyInAnyOrder(FieldErrors.tooLong("first_name", TEXT_MAX),
                            FieldErrors.tooLong("last_name", TEXT_MAX), FieldErrors.tooLong("title", TEXT_MAX));
        }

        @Test
        void answersNotFoundBeforeValidatingTheBody() {
            assertApiError(() -> service.invite(SYNDICATE_ID, invite(null, null, null, null)), ErrorCode.NOT_FOUND);
            assertNobodyInvited();
        }

        @Test
        void answersNotFoundForAnUnknownSyndicateWithoutCreatingAnAccount() {
            assertApiError(() -> service.invite(SYNDICATE_ID, invite("Layla", "Khoury", "a@b.org", "Registrar")),
                    ErrorCode.NOT_FOUND);
            assertNobodyInvited();
        }

        @Test
        void passesOnAnEmailThatIsAlreadyRegisteredWithoutAddingAMember() {
            when(staff.syndicateExists(SYNDICATE_ID)).thenReturn(true);
            when(invites.createPendingAccount(any(), any(), any(), any())).thenThrow(Errors.emailTaken());

            assertApiError(() -> service.invite(SYNDICATE_ID, invite("Layla", "Khoury", "a@b.org", "Registrar")),
                    ErrorCode.EMAIL_TAKEN);
            verify(staff, never()).insertMember(any(), any(), any());
            verify(invites, never()).sendInvite(any(), any());
        }
    }

    @Nested
    class UpdateTitle {

        @Test
        void savesTheStrippedTitleAndReturnsTheFreshRow() {
            givenMember();
            when(staff.updateTitle(STAFF_ID, SYNDICATE_ID, "Registrar")).thenReturn(true);

            SyndicateStaffResponse response = service.updateTitle(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffUpdateRequest(" \u00A0Registrar "));

            verify(staff).updateTitle(STAFF_ID, SYNDICATE_ID, "Registrar");
            assertThat(response.id()).isEqualTo(STAFF_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void requiresATitle(String blank) {
            givenMember();

            assertThat(errorsOf(() -> service.updateTitle(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffUpdateRequest(blank)))).containsExactly(FieldErrors.required("title"));
            verify(staff, never()).updateTitle(any(), any(), any());
        }

        @Test
        void requiresATitleThatIsNotNull() {
            givenMember();

            assertThat(errorsOf(() -> service.updateTitle(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffUpdateRequest(null)))).containsExactly(FieldErrors.required("title"));
        }

        @Test
        void rejectsATitleOneCharacterOverTheMaximum() {
            givenMember();

            assertThat(errorsOf(() -> service.updateTitle(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffUpdateRequest("t".repeat(TEXT_MAX + 1)))))
                    .containsExactly(FieldErrors.tooLong("title", TEXT_MAX));
        }

        @Test
        void answersNotFoundBeforeValidatingTheTitle() {
            assertApiError(() -> service.updateTitle(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffUpdateRequest("")), ErrorCode.NOT_FOUND);
            verify(staff, never()).updateTitle(any(), any(), any());
        }

        @Test
        void answersNotFoundForAStaffIdThatIsNotInThisSyndicate() {
            assertApiError(() -> service.updateTitle(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffUpdateRequest("Registrar")), ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class ChangePendingEmail {

        @Test
        void repointsThePendingAccountAndSendsTheFreshInviteAfterTheTransaction() {
            givenMember();
            when(invites.changePendingEmail(STAFF_USER_ID, "New@Order.org")).thenReturn(INVITE);

            SyndicateStaffResponse response = service.changePendingEmail(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffEmailRequest("New@Order.org"));

            InOrder order = inOrder(tx, invites);
            order.verify(tx).execute(any());
            order.verify(invites).sendInvite(INVITE, LocaleCode.EN);
            assertThat(response.id()).isEqualTo(STAFF_ID);
        }

        @Test
        void worksForTheOwnerToo() {
            SyndicateStaffRow owner = StaffRows.owner();
            when(staff.findInSyndicate(OWNER_STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(owner));
            when(assembler.toResponse(owner)).thenReturn(StaffRows.response(owner));
            when(invites.changePendingEmail(OWNER_USER_ID, "owner@order.org"))
                    .thenReturn(new Invite(OWNER_USER_ID, "owner@order.org", null));

            assertThat(service.changePendingEmail(SYNDICATE_ID, OWNER_STAFF_ID,
                    new SyndicateStaffEmailRequest("owner@order.org")).owner()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\u00A0"})
        void requiresAnEmail(String blank) {
            givenMember();

            assertThat(errorsOf(() -> service.changePendingEmail(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffEmailRequest(blank)))).containsExactly(FieldErrors.required("email"));
            verify(invites, never()).changePendingEmail(any(), any());
            verify(invites, never()).sendInvite(any(), any());
        }

        @Test
        void rejectsAnEmailThatIsNotAnEmail() {
            givenMember();

            assertThat(errorsOf(() -> service.changePendingEmail(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffEmailRequest("nope")))).containsExactly(FieldErrors.mustBeEmail("email"));
            verify(invites, never()).changePendingEmail(any(), any());
        }

        @Test
        void answersNotFoundBeforeValidatingTheEmail() {
            assertApiError(() -> service.changePendingEmail(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffEmailRequest("nope")), ErrorCode.NOT_FOUND);
            verify(invites, never()).changePendingEmail(any(), any());
        }

        @Test
        void answersNotFoundBeforeTouchingAnyAccount() {
            assertApiError(() -> service.changePendingEmail(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffEmailRequest("a@b.org")), ErrorCode.NOT_FOUND);
            verify(invites, never()).changePendingEmail(any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ACCOUNT_ALREADY_ACTIVE", "EMAIL_TAKEN"})
        void passesOnTheAccountErrorAndSendsNoInvite(String code) {
            givenMember();
            ApiException failure = new ApiException(ErrorCode.valueOf(code));
            when(invites.changePendingEmail(any(), any())).thenThrow(failure);

            assertThat(catchThrowable(() -> service.changePendingEmail(SYNDICATE_ID, STAFF_ID,
                    new SyndicateStaffEmailRequest("a@b.org")))).isSameAs(failure);
            verify(invites, never()).sendInvite(any(), any());
        }
    }

    @Nested
    class Remove {

        @Test
        void deletesTheMembershipThenTheAccount() {
            givenMember();

            service.remove(SYNDICATE_ID, STAFF_ID);

            InOrder order = inOrder(staff, accounts);
            order.verify(staff).delete(STAFF_ID);
            order.verify(accounts).deleteAccount(STAFF_USER_ID, UserRole.SYNDICATE_STAFF);
        }

        @Test
        void refusesToRemoveTheOwnerAndDeletesNothing() {
            when(staff.findInSyndicate(OWNER_STAFF_ID, SYNDICATE_ID)).thenReturn(Optional.of(StaffRows.owner()));

            assertApiError(() -> service.remove(SYNDICATE_ID, OWNER_STAFF_ID), ErrorCode.OWNER_CANNOT_BE_REMOVED);
            verify(staff, never()).delete(any());
            verify(accounts, never()).deleteAccount(any(), any());
        }

        @Test
        void answersNotFoundForAStaffIdThatIsNotInThisSyndicateAndDeletesNothing() {
            assertApiError(() -> service.remove(SYNDICATE_ID, STAFF_ID), ErrorCode.NOT_FOUND);
            verify(staff, never()).delete(any());
            verify(accounts, never()).deleteAccount(any(), any());
        }
    }
}