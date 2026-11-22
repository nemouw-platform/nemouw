package com.nemouw.specialist.discovery;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import com.nemouw.specialist.discovery.DiscoveryDtos.SearchResultLocation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class SpecialistSearchRepository {
    record Row(UUID id, String username, String firstName, String lastName, UUID specialtyId,
               String profilePictureKey, Integer yearsOfExperience, String primaryLanguage,
               BigDecimal sessionPriceMin, BigDecimal sessionPriceMax, String priceCurrency,
               Integer minAgeTreated, Integer maxAgeTreated, String title,
               String headline, String city, UUID countryId, Double distanceKm) {
    }

    private record ConditionLink(UUID specialistId, UUID conditionId) {
    }

    private record LocationLink(UUID specialistId, UUID countryId, String city, boolean isPrimary) {
    }

    private final JdbcClient jdbc;

    SpecialistSearchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<Row> page(SearchSql.Query q) {
        return jdbc.sql(q.dataSql()).params(q.params()).query(Row.class).list();
    }

    long count(SearchSql.Query q) {
        return jdbc.sql(q.countSql()).params(q.params()).query(Long.class).single();
    }

    Map<UUID, List<UUID>> conditionIds(Collection<UUID> specialistIds) {
        return groupedBySpecialist("""
            SELECT specialist_id, condition_id FROM specialist_treated_conditions
            WHERE specialist_id IN (:ids) ORDER BY condition_id""",
                specialistIds, ConditionLink.class, ConditionLink::specialistId, ConditionLink::conditionId);
    }

    Map<UUID, List<SearchResultLocation>> locations(Collection<UUID> specialistIds) {
        return groupedBySpecialist("""
            SELECT specialist_id, country_id, city, is_primary FROM specialist_locations
            WHERE specialist_id IN (:ids) ORDER BY is_primary DESC, created_at, id""",
                specialistIds, LocationLink.class, LocationLink::specialistId,
                l -> new SearchResultLocation(l.countryId(), l.city(), l.isPrimary()));
    }

    private <R, V> Map<UUID, List<V>> groupedBySpecialist(String sql, Collection<UUID> specialistIds, Class<R> row,
                                                          Function<R, UUID> owner, Function<R, V> value) {
        if (specialistIds.isEmpty()) return Map.of();
        return jdbc.sql(sql).param("ids", specialistIds).query(row).list().stream()
                .collect(Collectors.groupingBy(owner, Collectors.mapping(value, Collectors.toList())));
    }
}
