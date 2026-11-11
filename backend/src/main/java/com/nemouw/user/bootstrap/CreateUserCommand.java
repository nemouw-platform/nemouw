package com.nemouw.user.bootstrap;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.user.bootstrap.CreateUserService.NewUser;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** Runs only with {@code --create-user}: prompts for the details, sends the invitation, then exits. */
@Component
class CreateUserCommand implements ApplicationRunner {
    private static final String OPTION = "create-user";

    private final CreateUserService service;
    private final Terminal terminal;
    private final ApplicationContext context;

    CreateUserCommand(CreateUserService service, Terminal terminal, ApplicationContext context) {
        this.service = service;
        this.terminal = terminal;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION)) return;
        int exitCode = execute();
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    int execute() {
        try {
            return prompt();
        } catch (ValidationException e) {
            e.fieldErrors().forEach(f -> terminal.print(f.field() + " " + f.message()));
            return 1;
        } catch (ApiException | IllegalStateException e) {
            terminal.print(e.getMessage());
            return 1;
        }
    }

    private int prompt() {
        NewUser user = new NewUser(
                terminal.ask("Role (" + service.creatableRoles() + ")"),
                terminal.ask("Email"),
                terminal.ask("First name"),
                terminal.ask("Last name"),
                terminal.ask("Invite email language (en, ar, fr) [en]"));
        service.validate(user);

        if (!terminal.confirm("Send a " + user.role() + " invitation to " + user.email() + "?")) {
            terminal.print("Cancelled.");
            return 1;
        }
        service.create(user);
        terminal.print("Invitation sent to " + user.email() + ".");
        return 0;
    }
}