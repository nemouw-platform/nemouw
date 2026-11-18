package com.nemouw.specialist.profile;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecialistConstraintNamesTest {

    private static final List<String> CONSTRAINTS = List.of("uq_specialists_syndicate_member",
            "ck_specialists_age_range", "ck_specialists_price_range", "uq_user_identifiers_type_identifier");
    private static final List<String> TRIGGER_MESSAGES = List.of("syndicate_id cannot change",
            "syndicate_member_number cannot change");

    private static String migrations() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql");
        assertThat(files).isNotEmpty();
        StringBuilder sql = new StringBuilder();
        for (Resource file : files) sql.append(file.getContentAsString(StandardCharsets.UTF_8)).append('\n');
        return sql.toString();
    }

    @Test
    void everyNameTheServiceMatchesOnIsDeclaredInTheMigrationsAndNeverDropped() throws IOException {
        String sql = migrations();

        assertThat(CONSTRAINTS).allSatisfy(name -> {
            assertThat(sql).as(name).contains(name);
            assertThat(sql).as(name + " dropped or renamed")
                    .doesNotContain("DROP CONSTRAINT " + name, "DROP CONSTRAINT IF EXISTS " + name,
                            "RENAME CONSTRAINT " + name);
        });
        assertThat(TRIGGER_MESSAGES).allSatisfy(message -> assertThat(sql).as(message).contains(message));
    }
}