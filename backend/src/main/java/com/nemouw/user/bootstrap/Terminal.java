package com.nemouw.user.bootstrap;

interface Terminal {
    String ask(String label);

    boolean confirm(String question);

    void print(String line);
}