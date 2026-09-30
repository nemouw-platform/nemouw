package com.nemouw.database;

import org.postgresql.util.PSQLException;

public final class Db {
    private Db() {}

    public static boolean isUniqueViolation(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof PSQLException p && "23505".equals(p.getSQLState())) return true;
        }
        return false;
    }
}