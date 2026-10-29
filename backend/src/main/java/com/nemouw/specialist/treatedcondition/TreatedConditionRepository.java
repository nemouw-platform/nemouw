package com.nemouw.specialist.treatedcondition;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
class TreatedConditionRepository {
    private final JdbcClient jdbc;

    TreatedConditionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<UUID> findIds(UUID specialistId) {
        return jdbc.sql("SELECT condition_id FROM specialist_treated_conditions WHERE specialist_id = :specialistId ORDER BY condition_id")
                .param("specialistId", specialistId)
                .query(UUID.class)
                .list();
    }

    boolean allExist(Set<UUID> conditionIds) {
        if (conditionIds.isEmpty()) return true;
        long found = jdbc.sql("SELECT count(*) FROM conditions WHERE id IN (:ids)")
                .param("ids", conditionIds)
                .query(Long.class)
                .single();
        return found == conditionIds.size();
    }

    /** Makes the stored set equal to {@code conditionIds}. */
    void replace(UUID specialistId, Set<UUID> conditionIds) {
        if (conditionIds.isEmpty()) {
            jdbc.sql("DELETE FROM specialist_treated_conditions WHERE specialist_id = :specialistId")
                    .param("specialistId", specialistId)
                    .update();
            return;
        }
        jdbc.sql("""
                        DELETE FROM specialist_treated_conditions
                        WHERE specialist_id = :specialistId AND condition_id NOT IN (:ids)
                        """)
                .param("specialistId", specialistId)
                .param("ids", conditionIds)
                .update();
        jdbc.sql("""
                        INSERT INTO specialist_treated_conditions (specialist_id, condition_id)
                        SELECT CAST(:specialistId AS uuid), c.id FROM conditions c WHERE c.id IN (:ids)
                        ON CONFLICT (specialist_id, condition_id) DO NOTHING
                        """)
                .param("specialistId", specialistId)
                .param("ids", conditionIds)
                .update();
    }
}