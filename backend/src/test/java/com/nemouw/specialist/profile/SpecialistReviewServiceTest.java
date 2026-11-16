package com.nemouw.specialist.profile;

import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.media.api.PublicMediaUrls;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistRejectionRequest;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistResponse;
import com.nemouw.user.api.AccountService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.nemouw.specialist.profile.SpecialistVerificationStatus.ACTIVE;
import static com.nemouw.specialist.profile.SpecialistVerificationStatus.PENDING;
import static com.nemouw.support.Fixtures.NOW;
import static com.nemouw.support.Fixtures.OTHER_USER_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpecialistReviewServiceTest {
    private static final UUID SPECIALIST_ID = UUID.fromString("5d0a7c34-81b2-4e69-a3f7-2c9e6b14d085");
    private static final UUID REVIEWER_ID = OTHER_USER_ID;

    private final SpecialistRepository specialists = mock(SpecialistRepository.class);
    private final AccountService accounts = mock(AccountService.class);
    private final SpecialistReviewService service =
            new SpecialistReviewService(specialists, accounts, mock(PublicMediaUrls.class));

    @Test
    void verifiesAPendingSpecialist() {
        when(specialists.lockById(SPECIALIST_ID)).thenReturn(Optional.of(row(PENDING)));
        when(specialists.findById(SPECIALIST_ID)).thenReturn(Optional.of(row(ACTIVE)));

        SpecialistResponse response = service.verify(SPECIALIST_ID, REVIEWER_ID);

        verify(specialists).markVerified(SPECIALIST_ID, REVIEWER_ID);
        assertThat(response.verificationStatus()).isEqualTo(ACTIVE);
    }

    @Test
    void verifyingAnAlreadyVerifiedSpecialistChangesNothing() {
        when(specialists.lockById(SPECIALIST_ID)).thenReturn(Optional.of(row(ACTIVE)));

        service.verify(SPECIALIST_ID, REVIEWER_ID);

        verify(specialists, never()).markVerified(any(), any());
    }

    @Test
    void reportsAnUnknownSpecialistAsNotFound() {
        when(specialists.lockById(SPECIALIST_ID)).thenReturn(Optional.empty());

        assertApiError(() -> service.verify(SPECIALIST_ID, REVIEWER_ID), ErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsWithTheTrimmedReason() {
        when(specialists.lockById(SPECIALIST_ID)).thenReturn(Optional.of(row(PENDING)));
        when(specialists.findById(SPECIALIST_ID)).thenReturn(Optional.of(row(PENDING)));

        service.reject(SPECIALIST_ID, REVIEWER_ID, new SpecialistRejectionRequest("  Licence not found  "));

        verify(specialists).markRejected(SPECIALIST_ID, REVIEWER_ID, "Licence not found");
    }

    @Test
    void rejectionNeedsAReasonAndTouchesNothingWithoutOne() {
        assertThatThrownBy(() -> service.reject(SPECIALIST_ID, REVIEWER_ID, new SpecialistRejectionRequest("  ")))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(specialists);
    }

    @Test
    void suspendingChangesTheAccountAndNotTheVerification() {
        when(specialists.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(row(PENDING, UserStatus.ACTIVE)))
                .thenReturn(Optional.of(row(PENDING, UserStatus.SUSPENDED)));

        SpecialistResponse response = service.suspend(SPECIALIST_ID, REVIEWER_ID);

        verify(accounts).suspend(USER_ID);
        verify(specialists, never()).markVerified(any(), any());
        verify(specialists, never()).markRejected(any(), any(), any());
        assertThat(response.verificationStatus()).isEqualTo(PENDING);
        assertThat(response.userStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void unsuspendingChangesTheAccountAndNotTheVerification() {
        when(specialists.findById(SPECIALIST_ID))
                .thenReturn(Optional.of(row(ACTIVE, UserStatus.SUSPENDED)))
                .thenReturn(Optional.of(row(ACTIVE, UserStatus.ACTIVE)));

        SpecialistResponse response = service.unsuspend(SPECIALIST_ID, REVIEWER_ID);

        verify(accounts).unsuspend(USER_ID);
        verify(specialists, never()).markVerified(any(), any());
        verify(specialists, never()).markRejected(any(), any(), any());
        assertThat(response.verificationStatus()).isEqualTo(ACTIVE);
        assertThat(response.userStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void surfacesADeletedAccountWhenSuspending() {
        when(specialists.findById(SPECIALIST_ID)).thenReturn(Optional.of(row(ACTIVE)));
        doThrow(Errors.accountDeleted()).when(accounts).suspend(USER_ID);

        assertApiError(() -> service.suspend(SPECIALIST_ID, REVIEWER_ID), ErrorCode.ACCOUNT_DELETED);
    }

    private static SpecialistRow row(SpecialistVerificationStatus status) {
        return row(status, UserStatus.ACTIVE);
    }

    private static SpecialistRow row(SpecialistVerificationStatus status, UserStatus userStatus) {
        OffsetDateTime now = NOW.atOffset(ZoneOffset.UTC);
        return new SpecialistRow(SPECIALIST_ID, USER_ID, null, null, "M-1001", "dr.amira",
                null, null, null, null, null, null, null, null, null, null,
                status.code(), null, null, userStatus.code(), null, null, "USD", now, now);
    }
}