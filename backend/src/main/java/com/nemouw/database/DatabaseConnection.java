package com.nemouw.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class DatabaseConnection {
    private static final HikariDataSource hikariDataSource;

    static {
        Dotenv dotenv = Dotenv.load();
        String dbUrl = dotenv.get("DB_URL");

        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            throw new IllegalStateException("FATAL: DB_URL environment variable is missing or empty!");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setMaximumPoolSize(15);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setIdleTimeout(TimeUnit.SECONDS.toMillis(30));
        hikariConfig.setMaxLifetime(TimeUnit.MINUTES.toMillis(15));
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
        hikariConfig.setLeakDetectionThreshold(TimeUnit.MINUTES.toMillis(10));
        hikariDataSource = new HikariDataSource(hikariConfig);
    }

    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    // Expose the DataSource directly so Flyway can hook into it
    public static DataSource getDataSource() {
        return hikariDataSource;
    }
}
