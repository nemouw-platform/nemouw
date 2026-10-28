package com.nemouw.user.api;

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

    /**
     * Permanently deletes an account of the given role, with everything under it.
     * A wrong id can never remove an admin or a specialist
     */
    @Transactional(propagation = Propagation.MANDATORY)

    public void deleteAccount(UUID userId, UserRole role) {
        users.deleteByIdAndRole(userId, role);
    }
}