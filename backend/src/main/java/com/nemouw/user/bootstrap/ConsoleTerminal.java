package com.nemouw.user.bootstrap;

import org.springframework.stereotype.Component;

import java.io.Console;

@Component
class ConsoleTerminal implements Terminal {
    @Override
    public String ask(String label) {
        String line = console().readLine("%s: ", label);
        return line == null ? "" : line.trim();
    }

    @Override
    public boolean confirm(String question) {
        return ask(question + " [y/N]").equalsIgnoreCase("y");
    }

    @Override
    public void print(String line) {
        console().printf("%s%n", line);
    }

    private static Console console() {
        Console console = System.console();
        if (console == null || !console.isTerminal()) {
            throw new IllegalStateException("No interactive terminal available. Run the command with a TTY (e.g. docker exec -it).");
        }
        return console;
    }
}