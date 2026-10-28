package com.nemouw.country;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public final class CountryDtos {
    private CountryDtos() {}

    /** Used for create and update. On update, null means "leave unchanged". */
    public record CountryWriteRequest(String name, String isoCode, String currencyCode, String phoneCode,
                                      String region, @JsonProperty("is_active") Boolean active) {}

    public record CountryResponse(UUID id, String name, String isoCode, String currencyCode, String phoneCode,
                                  String region, @JsonProperty("is_active") boolean active,
                                  Instant createdAt, Instant updatedAt) {
        static CountryResponse from(Country c) {
            return new CountryResponse(c.getId(), c.getName(), c.getIsoCode(), c.getCurrencyCode(),
                    c.getPhoneCode(), c.getRegion(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}