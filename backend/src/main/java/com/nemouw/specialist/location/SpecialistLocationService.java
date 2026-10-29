package com.nemouw.specialist.location;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.location.SpecialistLocationDtos.SpecialistLocationResponse;
import com.nemouw.specialist.location.SpecialistLocationDtos.SpecialistLocationWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class SpecialistLocationService {
    private static final int MAX_LATITUDE = 90;
    private static final int MAX_LONGITUDE = 180;

    private final SpecialistLocationRepository locations;

    SpecialistLocationService(SpecialistLocationRepository locations) {
        this.locations = locations;
    }

    @Transactional(readOnly = true)
    List<SpecialistLocationResponse> list(UUID specialistId) {
        return locations.findAll(specialistId).stream().map(SpecialistLocationResponse::from).toList();
    }

    @Transactional
    SpecialistLocationResponse add(UUID specialistId, SpecialistLocationWriteRequest req) {
        LocationFields fields = validated(req);
        replacePrimaryIfRequested(specialistId, fields);
        return SpecialistLocationResponse.from(locations.insert(specialistId, fields));
    }

    @Transactional
    SpecialistLocationResponse update(UUID specialistId, UUID locationId, SpecialistLocationWriteRequest req) {
        LocationFields fields = validated(req);
        replacePrimaryIfRequested(specialistId, fields);
        return locations.update(specialistId, locationId, fields)
                .map(SpecialistLocationResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    void delete(UUID specialistId, UUID locationId) {
        if (!locations.delete(specialistId, locationId)) throw new ApiException(ErrorCode.NOT_FOUND);
    }

    private void replacePrimaryIfRequested(UUID specialistId, LocationFields fields) {
        if (!fields.primary()) return;
        locations.lockOwner(specialistId);
        locations.clearPrimary(specialistId);
    }

    private LocationFields validated(SpecialistLocationWriteRequest req) {
        new Validator()
                .notNull("country_id", req.countryId())
                .required("city", req.city())
                .maxLength("city", req.city(), TEXT_MAX)
                .maxLength("label", req.label(), TEXT_MAX)
                .maxLength("address_line1", req.addressLine1(), TEXT_MAX)
                .maxLength("address_line2", req.addressLine2(), TEXT_MAX)
                .maxLength("phone", req.phone(), TEXT_MAX)
                .check(within(req.latitude(), MAX_LATITUDE),
                        FieldErrors.invalidFormat("latitude", "must be between -90 and 90"))
                .check(within(req.longitude(), MAX_LONGITUDE),
                        FieldErrors.invalidFormat("longitude", "must be between -180 and 180"))
                .throwIfInvalid();
        if (!locations.countryExists(req.countryId())) throw Errors.invalidField("country_id", "unknown country");

        return new LocationFields(req.countryId(), blankToNull(req.label()), blankToNull(req.addressLine1()),
                blankToNull(req.addressLine2()), req.city().trim(), req.latitude(), req.longitude(),
                blankToNull(req.phone()), Boolean.TRUE.equals(req.primary()));
    }

    private static boolean within(BigDecimal value, int limit) {
        return value == null || value.abs().compareTo(BigDecimal.valueOf(limit)) <= 0;
    }
}