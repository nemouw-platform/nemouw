package com.nemouw.user.bootstrap;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import com.nemouw.user.bootstrap.CreateUserService.NewUser;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static com.nemouw.support.Fixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateUserServiceTest {
    private final InviteService invites = mock(InviteService.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final CreateUserService service = new CreateUserService(invites, tx);

    CreateUserServiceTest() {
        when(tx.execute(any())).thenAnswer(call ->
                ((TransactionCallback<?>) call.getArgument(0)).doInTransaction(null));
    }

    @Test
    void createsTheAccountWithTheChosenRoleAndInvitesInTheChosenLanguage() {
        Invite invite = new Invite(USER_ID, "amira@clinicmail.org", null);
        when(invites.createPendingAccount(UserRole.SPECIALIST, "Amira", "Haddad", "amira@clinicmail.org"))
                .thenReturn(invite);

        service.create(new NewUser("Specialist", "amira@clinicmail.org", "Amira", "Haddad", "ar"));

        verify(invites).sendInvite(invite, LocaleCode.AR);
    }

    @Test
    void defaultsTheInviteLanguageToEnglish() {
        Invite invite = new Invite(USER_ID, "amira@clinicmail.org", null);
        when(invites.createPendingAccount(UserRole.ADMIN, "Amira", "Haddad", "amira@clinicmail.org"))
                .thenReturn(invite);

        service.create(new NewUser("admin", "amira@clinicmail.org", "Amira", "Haddad", ""));

        verify(invites).sendInvite(invite, LocaleCode.EN);
    }

    @Test
    void refusesSyndicateStaffBecauseTheyNeedAMembership() {
        assertThatThrownBy(() -> service.create(
                new NewUser("syndicate_staff", "amira@clinicmail.org", "Amira", "Haddad", "")))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(invites);
    }

    @Test
    void refusesAnUnknownLanguage() {
        assertThatThrownBy(() -> service.create(
                new NewUser("admin", "amira@clinicmail.org", "Amira", "Haddad", "xx")))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(invites);
    }
}