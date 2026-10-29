package com.nemouw.specialist.location;

import java.math.BigDecimal;
import java.util.UUID;

/** A validated, normalized location write. */
record LocationFields(UUID countryId, String label, String addressLine1, String addressLine2, String city,
                      BigDecimal latitude, BigDecimal longitude, String phone, boolean primary) {
}