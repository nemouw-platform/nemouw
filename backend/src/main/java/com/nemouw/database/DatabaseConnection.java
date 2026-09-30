package com.nemouw.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class DatabaseConnection {
    private static final HikariDataSource hikariDataSource;
    private static final Jdbi jdbi;

    static {
        Dotenv env = Dotenv.load();
        String dbUrl = env.get("DB_URL");

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

        // JDBI borrows connections from the Hikari pool and returns them when each handle closes
        jdbi = Jdbi.create(hikariDataSource).installPlugin(new PostgresPlugin());
    }

    /**
     * Main entry point for queries.
     */
    public static Jdbi jdbi() {
        return jdbi;
    }

    /**
     * Raw JDBC connection, for the rare case JDBI doesn't fit. Caller must close it.
     */
    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    /**
     * Expose the DataSource directly so Flyway can hook into it.
     */
    public static DataSource getDataSource() {
        return hikariDataSource;
    }
}