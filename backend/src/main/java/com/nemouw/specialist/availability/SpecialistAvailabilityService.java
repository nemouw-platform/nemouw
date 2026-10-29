package com.nemouw.specialist.availability;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.availability.SpecialistAvailabilityDtos.SpecialistAvailabilityResponse;
import com.nemouw.specialist.availability.SpecialistAvailabilityDtos.SpecialistAvailabilityWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
class SpecialistAvailabilityService {
    private final SpecialistAvailabilityRepository availabilities;

    SpecialistAvailabilityService(SpecialistAvailabilityRepository availabilities) {
        this.availabilities = availabilities;
    }

    @Transactional(readOnly = true)
    List<SpecialistAvailabilityResponse> list(UUID specialistId) {
        return availabilities.findAll(specialistId).stream().map(SpecialistAvailabilityResponse::from).toList();
    }

    @Transactional
    SpecialistAvailabilityResponse add(UUID specialistId, SpecialistAvailabilityWriteRequest req) {
        AvailabilityFields fields = validated(specialistId, req);
        return SpecialistAvailabilityResponse.from(availabilities.insert(specialistId, fields));
    }

    @Transactional
    SpecialistAvailabilityResponse update(UUID specialistId, UUID availabilityId,
                                          SpecialistAvailabilityWriteRequest req) {
        AvailabilityFields fields = validated(specialistId, req);
        return availabilities.update(specialistId, availabilityId, fields)
                .map(SpecialistAvailabilityResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    void delete(UUID specialistId, UUID availabilityId) {
        if (!availabilities.delete(specialistId, availabilityId)) throw new ApiException(ErrorCode.NOT_FOUND);
    }

    private AvailabilityFields validated(UUID specialistId, SpecialistAvailabilityWriteRequest req) {
        Validator v = new Validator().notNull("day_of_week", req.dayOfWeek());
        if (req.dayOfWeek() != null) {
            v.check(req.dayOfWeek() >= 0 && req.dayOfWeek() <= 6,
                    FieldErrors.invalidFormat("day_of_week", "must be between 0 and 6"));
        }
        LocalTime start = parseTime(v, "start_time", req.startTime());
        LocalTime end = parseTime(v, "end_time", req.endTime());
        if (start != null && end != null) {
            v.check(end.isAfter(start), FieldErrors.invalidFormat("end_time", "must be after start_time"));
        }
        v.throwIfInvalid();

        if (req.locationId() != null && !availabilities.locationBelongsTo(specialistId, req.locationId())) {
            throw Errors.invalidField("location_id", "unknown location");
        }
        return new AvailabilityFields(req.locationId(), req.dayOfWeek(), start, end);
    }

    private static LocalTime parseTime(Validator v, String field, String text) {
        if (text == null || text.isBlank()) {
            v.required(field, text);
            return null;
        }
        LocalTime time = ClockTime.parse(text);
        v.check(time != null, FieldErrors.invalidFormat(field, "must be a time as HH:mm"));
        return time;
    }
}