package com.nemouw.specialist.location;

import java.math.BigDecimal;
import java.util.UUID;

record SpecialistLocationRow(UUID id, UUID countryId, String label, String addressLine1, String addressLine2,
                             String city, BigDecimal latitude, BigDecimal longitude, String phone,
                             boolean isPrimary) {
}