package com.nemouw.user.management;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.user.api.AccountService;
import com.nemouw.user.management.ManagementDtos.UserDetail;
import com.nemouw.user.management.ManagementDtos.UserPage;
import com.nemouw.user.management.UserListParser.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Admin actions on accounts of any role. Authorization is on the security chain; these methods assume it has passed. */
@Service
class UserManagementService {
    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

    private final ManagedUserRepository users;
    private final AccountService accounts;
    private final UserUpdateParser updates;

    UserManagementService(ManagedUserRepository users, AccountService accounts, UserUpdateParser updates) {
        this.users = users;
        this.accounts = accounts;
        this.updates = updates;
    }

    @Transactional(readOnly = true)
    UserPage list(MultiValueMap<String, String> params) {
        Query query = UserListParser.parse(params);
        List<UserDetail> data = users.search(query.filter(), query.pageSize(), query.offset()).stream()
                .map(UserDetail::from).toList();
        return new UserPage(data, query.page(), query.pageSize(), users.count(query.filter()));
    }

    @Transactional(readOnly = true)
    UserDetail get(UUID userId) {
        return UserDetail.from(users.findById(userId).orElseThrow(UserManagementService::notFound));
    }

    @Transactional
    UserDetail update(UUID userId, Map<String, Object> body) {
        lock(userId);
        users.update(userId, updates.parse(body));
        return reload(userId);
    }

    @Transactional
    UserDetail suspend(UUID userId) {
        ManagedUserRow user = lock(userId);
        if (!isModeratable(user)) throw Errors.cannotSuspendTarget();
        if (user.userStatus() == UserStatus.PENDING) throw Errors.cannotSuspendPending();
        accounts.suspend(userId);
        log.info("USER_SUSPENDED user={}", userId);
        return reload(userId);
    }

    @Transactional
    UserDetail unsuspend(UUID userId) {
        if (!isModeratable(lock(userId))) throw Errors.cannotUnsuspendTarget();
        accounts.unsuspend(userId);
        log.info("USER_UNSUSPENDED user={}", userId);
        return reload(userId);
    }

    private ManagedUserRow lock(UUID userId) {
        return users.lockById(userId).orElseThrow(UserManagementService::notFound);
    }

    private UserDetail reload(UUID userId) {
        return UserDetail.from(users.findById(userId).orElseThrow());
    }

    /** Admins have their own endpoints, and a deleted account is only waiting for its purge. */
    private static boolean isModeratable(ManagedUserRow user) {
        return user.role() != UserRole.ADMIN && user.userStatus() != UserStatus.DELETED;
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }
}