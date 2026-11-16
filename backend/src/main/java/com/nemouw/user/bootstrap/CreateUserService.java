package com.nemouw.user.bootstrap;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nemouw.common.util.ValidationLimits.NAME_MAX_LENGTH;

@Service
class CreateUserService {
    /** Syndicate staff are excluded: they need a syndicate membership row, created by the staff API. */
    private static final Set<UserRole> CREATABLE = EnumSet.of(UserRole.ADMIN, UserRole.SPECIALIST, UserRole.PATIENT);

    record NewUser(String role, String email, String firstName, String lastName, String language) {
    }

    private final InviteService invites;
    private final TransactionTemplate tx;

    CreateUserService(InviteService invites, TransactionTemplate tx) {
        this.invites = invites;
        this.tx = tx;
    }

    String creatableRoles() {
        return CREATABLE.stream().map(UserRole::code).collect(Collectors.joining(", "));
    }

    void validate(NewUser user) {
        new Validator()
                .check(parseRole(user.role()).isPresent(),
                        FieldErrors.invalidFormat("role", "must be one of: " + creatableRoles()))
                .requiredEmail("email", user.email())
                .required("first_name", user.firstName())
                .maxLength("first_name", user.firstName(), NAME_MAX_LENGTH)
                .required("last_name", user.lastName())
                .maxLength("last_name", user.lastName(), NAME_MAX_LENGTH)
                .check(user.language().isBlank() || parseLocale(user.language()).isPresent(),
                        FieldErrors.invalidFormat("language", "must be one of: en, ar, fr"))
                .throwIfInvalid();
    }

    void create(NewUser user) {
        validate(user);
        UserRole role = parseRole(user.role()).orElseThrow();
        Invite invite = Objects.requireNonNull(tx.execute(status ->
                invites.createPendingAccount(role, user.firstName(), user.lastName(), user.email())));
        invites.sendInvite(invite, parseLocale(user.language()).orElse(LocaleCode.EN));
    }

    private static Optional<UserRole> parseRole(String value) {
        return UserRole.tryFrom(value.trim().toLowerCase()).filter(CREATABLE::contains);
    }

    private static Optional<LocaleCode> parseLocale(String value) {
        return LocaleCode.tryFrom(value.trim().toLowerCase());
    }
}