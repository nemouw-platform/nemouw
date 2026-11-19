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
import static com.nemouw.common.util.TextNormalizer.strip;
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

    /**
     * A specialist with any location has exactly one primary. The first location is always the primary one,
     * whatever the request says; a later one becomes primary only when asked, and then replaces the old one.
     */
    @Transactional
    SpecialistLocationResponse add(UUID specialistId, SpecialistLocationWriteRequest req) {
        LocationFields fields = validated(req);
        locations.lockOwner(specialistId);
        rejectDuplicate(specialistId, null, fields);

        boolean primary = Boolean.TRUE.equals(req.primary()) || locations.count(specialistId) == 0;
        if (primary) locations.clearPrimary(specialistId);
        return SpecialistLocationResponse.from(locations.insert(specialistId, fields, primary));
    }

    @Transactional
    SpecialistLocationResponse update(UUID specialistId, UUID locationId, SpecialistLocationWriteRequest req) {
        LocationFields fields = validated(req);
        locations.lockOwner(specialistId);
        SpecialistLocationRow current = locations.find(specialistId, locationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        rejectDuplicate(specialistId, locationId, fields);

        boolean primary = req.primary() == null ? current.isPrimary() : req.primary();
        if (current.isPrimary() && !primary) throw Errors.primaryLocationRequired();
        if (primary && !current.isPrimary()) locations.clearPrimary(specialistId);

        return locations.update(specialistId, locationId, fields, primary)
                .map(SpecialistLocationResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    void delete(UUID specialistId, UUID locationId) {
        locations.lockOwner(specialistId);
        SpecialistLocationRow current = locations.find(specialistId, locationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (current.isPrimary() && locations.count(specialistId) > 1) throw Errors.primaryLocationRequired();
        if (!locations.delete(specialistId, locationId)) throw new ApiException(ErrorCode.NOT_FOUND);
    }

    private void rejectDuplicate(UUID specialistId, UUID excludeId, LocationFields fields) {
        if (locations.existsDuplicate(specialistId, excludeId, fields)) throw Errors.locationAlreadyExists();
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
                blankToNull(req.addressLine2()), strip(req.city()), req.latitude(), req.longitude(),
                blankToNull(req.phone()));
    }

    private static boolean within(BigDecimal value, int limit) {
        return value == null || value.abs().compareTo(BigDecimal.valueOf(limit)) <= 0;
    }
}