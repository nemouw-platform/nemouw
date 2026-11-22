package com.nemouw.specialist.discovery;

import com.nemouw.specialist.discovery.DiscoveryDtos.PublicAvailability;
import com.nemouw.specialist.discovery.DiscoveryDtos.PublicLocation;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PublicProfileRepository {
    record Profile(UUID id, String username, String firstName, String lastName, UUID specialtyId, String gender,
                   Integer yearsOfExperience, String primaryLanguage, String whatsappNumber,
                   Integer minAgeTreated, Integer maxAgeTreated,
                   BigDecimal sessionPriceMin, BigDecimal sessionPriceMax, String priceCurrency,
                   String profilePictureKey, String coverImageKey, UUID syndicateId, String syndicateName,
                   String contentLocale, String title, String headline, String bio) {
    }

    record Credential(String credentialType, String title, String issuingOrganization, LocalDate issueDate,
                      LocalDate expiryDate, OffsetDateTime verifiedAt) {
    }

    private static final String PROFILE = """
            SELECT s.id, s.username, u.first_name, u.last_name, s.specialty_id, s.gender,
                   s.years_of_experience, s.primary_language, s.whatsapp_number,
                   s.min_age_treated, s.max_age_treated,
                   s.session_price_min, s.session_price_max, s.price_currency,
                   pm.s3_key AS profile_picture_key, cm.s3_key AS cover_image_key,
                   s.syndicate_id, sy.name AS syndicate_name,
                   pc.locale_code AS content_locale, pc.title, pc.headline, pc.bio
            FROM specialists s
            JOIN users u ON u.id = s.user_id
            JOIN syndicates sy ON sy.id = s.syndicate_id
            LEFT JOIN media pm ON pm.id = s.profile_picture_id
            LEFT JOIN media cm ON cm.id = s.cover_image_id
            %s
            WHERE s.username = :username AND %s""".formatted(PublicSql.contentJoin(true), PublicSql.DISCOVERABLE);

    private final JdbcClient jdbc;

    PublicProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Profile> find(String username, String locale) {
        return jdbc.sql(PROFILE)
                .param("username", username)
                .param("locale", locale)
                .param("defaultLocale", PublicSql.DEFAULT_LOCALE)
                .query(Profile.class).optional();
    }

    List<UUID> conditionIds(UUID specialistId) {
        return jdbc.sql("SELECT condition_id FROM specialist_treated_conditions "
                        + "WHERE specialist_id = :id ORDER BY condition_id")
                .param("id", specialistId).query(UUID.class).list();
    }

    List<PublicLocation> locations(UUID specialistId) {
        return jdbc.sql("""
                        SELECT id, country_id, label, city, address_line1, address_line2, latitude, longitude,
                               phone, is_primary
                        FROM specialist_locations WHERE specialist_id = :id
                        ORDER BY is_primary DESC, created_at, id""")
                .param("id", specialistId).query(PublicLocation.class).list();
    }

    List<PublicAvailability> availabilities(UUID specialistId) {
        return jdbc.sql("SELECT id, location_id, day_of_week, to_char(start_time, 'HH24:MI') AS start_time, "
                        + "to_char(end_time, 'HH24:MI') AS end_time FROM specialist_location_availabilities "
                        + "WHERE specialist_id = :id ORDER BY " + PublicSql.weekdayPosition("day_of_week")
                        + ", start_time, id")
                .param("id", specialistId).query(PublicAvailability.class).list();
    }

    List<Credential> credentials(UUID specialistId, LocalDate today) {
        return jdbc.sql("""
                        SELECT credential_type, title, issuing_organization, issue_date, expiry_date,
                               reviewed_at AS verified_at
                        FROM specialist_credentials
                        WHERE specialist_id = :id AND verification_status = 'verified'
                          AND (expiry_date IS NULL OR expiry_date >= :today)
                        ORDER BY reviewed_at, id""")
                .param("id", specialistId).param("today", today).query(Credential.class).list();
    }
}
