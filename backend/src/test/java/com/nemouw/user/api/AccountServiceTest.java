package com.nemouw.user.api;

import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.user.identity.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AccountService service = new AccountService(users);

    @Test
    void suspendsAnActiveUser() {
        when(users.lockStatus(USER_ID)).thenReturn(Optional.of(UserStatus.ACTIVE));

        service.suspend(USER_ID);

        verify(users).changeStatus(USER_ID, UserStatus.ACTIVE, UserStatus.SUSPENDED);
    }

    @Test
    void unsuspendsOnlyFromSuspended() {
        when(users.lockStatus(USER_ID)).thenReturn(Optional.of(UserStatus.SUSPENDED));

        service.unsuspend(USER_ID);

        verify(users).changeStatus(USER_ID, UserStatus.SUSPENDED, UserStatus.ACTIVE);
    }

    @Test
    void refusesDeletedAccounts() {
        when(users.lockStatus(USER_ID)).thenReturn(Optional.of(UserStatus.DELETED));

        assertApiError(() -> service.suspend(USER_ID), ErrorCode.ACCOUNT_DELETED);
        assertApiError(() -> service.unsuspend(USER_ID), ErrorCode.ACCOUNT_DELETED);
        verify(users, never()).changeStatus(any(), any(), any());
    }

    @Test
    void reportsAnUnknownUserAsNotFound() {
        when(users.lockStatus(USER_ID)).thenReturn(Optional.empty());

        assertApiError(() -> service.suspend(USER_ID), ErrorCode.NOT_FOUND);
    }
}