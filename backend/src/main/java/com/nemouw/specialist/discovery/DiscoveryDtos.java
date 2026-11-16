package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.Currency;
import com.nemouw.common.enums.Gender;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.query.FieldSet;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DiscoveryDtos {
    private DiscoveryDtos() {
    }

    public record SpecialistSearchResult(UUID id, String username, String firstName, String lastName,
                                         String title, String headline, UUID specialtyId,
                                         String profilePictureUrl, Integer yearsOfExperience, String city,
                                         UUID countryId, Double distanceKm, LocaleCode primaryLanguage,
                                         BigDecimal sessionPriceMin, BigDecimal sessionPriceMax,
                                         Currency priceCurrency, Integer minAgeTreated, Integer maxAgeTreated,
                                         List<UUID> treatedConditionIds, List<SearchResultLocation> locations) {
    }

    public record SpecialistSearchPage(List<SpecialistSearchResult> data, int page, int pageSize, long total) {
    }

    public record SearchOptions(List<FieldSet.FieldInfo> fields, List<String> computedSorts) {
    }

    public record PublicLocation(UUID id, UUID countryId, String label, String city, String addressLine1,
                                 String addressLine2, BigDecimal latitude, BigDecimal longitude, String phone,
                                 boolean isPrimary) {
    }

    public record PublicAvailability(UUID id, UUID locationId, String dayOfWeek, String startTime, String endTime) {
    }

    public record SyndicateRef(UUID id, String name) {
    }

    public record PublicCredential(String credentialType, String title, String issuingOrganization,
                                   LocalDate issueDate, LocalDate expiryDate, Instant verifiedAt,
                                   SyndicateRef verifiedBySyndicate) {
    }

    public record SpecialistPublicProfile(UUID id, String username, String firstName, String lastName,
                                          LocaleCode locale, String title, String headline, String bio,
                                          UUID specialtyId, Gender gender, Integer yearsOfExperience,
                                          LocaleCode primaryLanguage, BigDecimal sessionPriceMin,
                                          BigDecimal sessionPriceMax, Currency priceCurrency,
                                          String whatsappNumber, Integer minAgeTreated, Integer maxAgeTreated,
                                          String profilePictureUrl,
                                          String coverImageUrl, List<UUID> treatedConditionIds,
                                          List<PublicLocation> locations, List<PublicAvailability> availabilities,
                                          List<PublicCredential> credentials) {
    }

    public record SearchResultLocation(UUID countryId, String city, boolean isPrimary) {
    }
}
