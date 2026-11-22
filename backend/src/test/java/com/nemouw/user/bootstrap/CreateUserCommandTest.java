package com.nemouw.user.bootstrap;

import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.user.bootstrap.CreateUserService.NewUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateUserCommandTest {
    private static final NewUser SPECIALIST =
            new NewUser("specialist", "amira@clinicmail.org", "Amira", "Haddad", "ar");

    private final CreateUserService service = mock(CreateUserService.class);
    private final FakeTerminal terminal =
            new FakeTerminal("specialist", "amira@clinicmail.org", "Amira", "Haddad", "ar");
    private final CreateUserCommand command =
            new CreateUserCommand(service, terminal, mock(ApplicationContext.class));

    CreateUserCommandTest() {
        when(service.creatableRoles()).thenReturn("admin, specialist, patient");
    }

    @Test
    void createsTheUserAfterConfirmation() {
        terminal.confirmation = true;

        assertThat(command.execute()).isZero();

        verify(service).create(SPECIALIST);
    }

    @Test
    void createsNothingWhenTheOperatorDeclines() {
        terminal.confirmation = false;

        assertThat(command.execute()).isEqualTo(1);

        verify(service, never()).create(any());
    }

    @Test
    void reportsInvalidInputBeforeAskingForConfirmation() {
        doThrow(new ValidationException(List.of(FieldErrors.required("email"))))
                .when(service).validate(any());

        assertThat(command.execute()).isEqualTo(1);

        assertThat(terminal.printed).anyMatch(line -> line.startsWith("email"));
        verify(service, never()).create(any());
    }

    @Test
    void reportsATakenEmailAndFails() {
        terminal.confirmation = true;
        doThrow(Errors.emailTaken()).when(service).create(any());

        assertThat(command.execute()).isEqualTo(1);

        assertThat(terminal.printed).isNotEmpty();
    }

    @Test
    void doesNothingWithoutTheOption() {
        command.run(new DefaultApplicationArguments());

        assertThat(terminal.asked).isEmpty();
    }

    private static final class FakeTerminal implements Terminal {
        private final Deque<String> answers = new ArrayDeque<>();
        private final List<String> asked = new ArrayList<>();
        private final List<String> printed = new ArrayList<>();
        private boolean confirmation;

        FakeTerminal(String... answers) {
            this.answers.addAll(List.of(answers));
        }

        @Override
        public String ask(String label) {
            asked.add(label);
            return answers.poll();
        }

        @Override
        public boolean confirm(String question) {
            return confirmation;
        }

        @Override
        public void print(String line) {
            printed.add(line);
        }
    }
}