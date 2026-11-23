package com.nemouw.user.management;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.util.Validator;
import com.nemouw.user.api.AccountService;
import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import com.nemouw.user.api.UserSummary;
import com.nemouw.user.management.ManagementDtos.AdminInviteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static com.nemouw.common.util.ValidationLimits.NAME_MAX_LENGTH;

@Service
class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private record Invited(UserSummary admin, Invite invite) {
    }

    private final ManagedUserRepository users;
    private final AccountService accounts;
    private final InviteService invites;
    private final TransactionTemplate tx;

    AdminService(ManagedUserRepository users, AccountService accounts, InviteService invites,
                 TransactionTemplate tx) {
        this.users = users;
        this.accounts = accounts;
        this.invites = invites;
        this.tx = tx;
    }

    UserSummary invite(AdminInviteRequest req) {
        new Validator()
                .required("first_name", req.firstName())
                .required("last_name", req.lastName())
                .requiredEmail("email", req.email())
                .maxLength("first_name", req.firstName(), NAME_MAX_LENGTH)
                .maxLength("last_name", req.lastName(), NAME_MAX_LENGTH)
                .throwIfInvalid();

        Invited created = tx.execute(status -> {
            Invite invite = invites.createPendingAccount(UserRole.ADMIN, req.firstName(), req.lastName(), req.email());
            return new Invited(users.findById(invite.userId()).orElseThrow().summary(), invite);
        });
        invites.sendInvite(created.invite(), LocaleCode.EN);   // only after the commit
        return created.admin();
    }

    @Transactional(readOnly = true)
    List<UserSummary> list() {
        return users.findByRole(UserRole.ADMIN).stream().map(ManagedUserRow::summary).toList();
    }

    @Transactional(readOnly = true)
    UserSummary get(UUID adminId) {
        return users.findById(adminId).filter(AdminService::isAdmin).orElseThrow(AdminService::notFound).summary();
    }

    /** Refuses the caller's own account, so the last active admin can never lock everyone out. */
    @Transactional
    UserSummary suspend(UUID adminId, UUID actorId) {
        ManagedUserRow admin = lockAdmin(adminId);
        if (adminId.equals(actorId)) throw Errors.cannotSuspendSelf();
        if (admin.userStatus() == UserStatus.PENDING) throw Errors.cannotSuspendPending();
        accounts.suspend(adminId);
        log.info("ADMIN_SUSPENDED admin={}", adminId);
        return reload(adminId);
    }

    @Transactional
    UserSummary unsuspend(UUID adminId) {
        lockAdmin(adminId);
        accounts.unsuspend(adminId);
        log.info("ADMIN_UNSUSPENDED admin={}", adminId);
        return reload(adminId);
    }

    private ManagedUserRow lockAdmin(UUID adminId) {
        return users.lockById(adminId).filter(AdminService::isAdmin).orElseThrow(AdminService::notFound);
    }

    private UserSummary reload(UUID adminId) {
        return users.findById(adminId).orElseThrow().summary();
    }

    private static boolean isAdmin(ManagedUserRow user) {
        return user.role() == UserRole.ADMIN;
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }
}