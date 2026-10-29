package com.nemouw.specialist.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public final class SpecialistLocationDtos {
    private SpecialistLocationDtos() {
    }

    public record SpecialistLocationWriteRequest(UUID countryId, String label, String addressLine1,
                                                 String addressLine2, String city, BigDecimal latitude,
                                                 BigDecimal longitude, String phone,
                                                 @JsonProperty("is_primary") Boolean primary) {
    }

    public record SpecialistLocationResponse(UUID id, UUID countryId, String label, String city,
                                             String addressLine1, String addressLine2, BigDecimal latitude,
                                             BigDecimal longitude, String phone,
                                             @JsonProperty("is_primary") boolean primary) {
        static SpecialistLocationResponse from(SpecialistLocationRow r) {
            return new SpecialistLocationResponse(r.id(), r.countryId(), r.label(), r.city(), r.addressLine1(),
                    r.addressLine2(), r.latitude(), r.longitude(), r.phone(), r.isPrimary());
        }
    }
}