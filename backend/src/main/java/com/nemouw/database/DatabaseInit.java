package com.nemouw.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import static com.nemouw.database.DatabaseConnection.getDataSource;

public class DatabaseInit {

    private DatabaseInit() {}

    /**
     * Applies all pending migrations from src/main/resources/db/migration.
     * Throws on failure so the app never starts against a half-migrated schema.
     */
    public static void initializeDB() {
        // Flyway handles checking what already exists and safely ignores executed changes!
        Flyway flyway = Flyway.configure()
                .dataSource(getDataSource())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();
        System.out.println("Flyway: applied " + result.migrationsExecuted
                + " migration(s), schema version " + result.targetSchemaVersion);
    }
}
