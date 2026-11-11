package com.nemouw.user.api;

import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.user.identity.UserRepository;
import com.nemouw.common.enums.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lifecycle of existing accounts, for other modules to call instead of reaching into the user module's repositories.
 */
@Service
public class AccountService {
    private final UserRepository users;

    AccountService(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void suspend(UUID userId) {
        requireNotDeleted(userId);
        users.changeStatus(userId, UserStatus.ACTIVE, UserStatus.SUSPENDED);
    }

    /** A pending account stays pending: only a suspended one becomes active. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void unsuspend(UUID userId) {
        requireNotDeleted(userId);
        users.changeStatus(userId, UserStatus.SUSPENDED, UserStatus.ACTIVE);
    }

    private void requireNotDeleted(UUID userId) {
        UserStatus status = users.lockStatus(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (status == UserStatus.DELETED) throw Errors.accountDeleted();
    }

    /**
     * Permanently deletes an account of the given role, with everything under it.
     * A wrong id can never remove an admin or a specialist
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAccount(UUID userId, UserRole role) {
        users.deleteByIdAndRole(userId, role);
    }
}