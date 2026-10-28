package com.nemouw.specialty;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.specialty.SpecialtyLookup.SpecialtyExistsProvider;
import com.nemouw.specialty.SpecialtyDtos.SpecialtyResponse;
import com.nemouw.specialty.SpecialtyDtos.SpecialtyWriteRequest;
import com.nemouw.common.util.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.DESCRIPTION_MAX;
import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class SpecialtyService {
    private static final Sort BY_NAME = Sort.by("name");

    private final SpecialtyRepository specialties;

    SpecialtyService(SpecialtyRepository specialties) {
        this.specialties = specialties;
    }

    @Transactional(readOnly = true)
    List<SpecialtyResponse> list(Boolean active) {
        List<Specialty> rows = active == null ? specialties.findAll(BY_NAME) : specialties.findByActive(active, BY_NAME);
        return rows.stream().map(SpecialtyResponse::from).toList();
    }

    @Transactional
    SpecialtyResponse create(SpecialtyWriteRequest req) {
        validate(req, true);
        if (specialties.existsByCode(normalizeCode(req.code()))) throw new ApiException(ErrorCode.SPECIALTY_CODE_TAKEN);

        Specialty specialty = Specialty.create();
        apply(specialty, req);
        try {
            return SpecialtyResponse.from(specialties.saveAndFlush(specialty));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.SPECIALTY_CODE_TAKEN);   // lost a race with another create
        }
    }

    @Transactional
    SpecialtyResponse update(UUID id, SpecialtyWriteRequest req) {
        validate(req, false);
        Specialty specialty = specialties.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (req.code() != null && specialties.existsByCodeAndIdNot(normalizeCode(req.code()), id)) {
            throw new ApiException(ErrorCode.SPECIALTY_CODE_TAKEN);
        }
        apply(specialty, req);
        try {
            specialties.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.SPECIALTY_CODE_TAKEN);
        }
        return SpecialtyResponse.from(specialty);
    }

    private static String normalizeCode(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Copies only the fields that were sent. Validation has already run.
     */
    private static void apply(Specialty s, SpecialtyWriteRequest r) {
        if (r.code() != null) s.setCode(normalizeCode(r.code()));
        if (r.name() != null) s.setName(r.name().trim());
        if (r.label() != null) s.setLabel(r.label().trim());
        if (r.labelFr() != null) s.setLabelFr(blankToNull(r.labelFr()));
        if (r.labelAr() != null) s.setLabelAr(blankToNull(r.labelAr()));
        if (r.description() != null) s.setDescription(blankToNull(r.description()));
        if (r.active() != null) s.setActive(r.active());
    }

    private static void validate(SpecialtyWriteRequest r, boolean create) {
        new Validator()
                .requiredForWrite(create, "code", r.code())
                .requiredForWrite(create, "name", r.name())
                .requiredForWrite(create, "label", r.label())
                .code("code", r.code())
                .maxLength("name", r.name(), TEXT_MAX)
                .maxLength("label", r.label(), TEXT_MAX)
                .maxLength("label_fr", r.labelFr(), TEXT_MAX)
                .maxLength("label_ar", r.labelAr(), TEXT_MAX)
                .maxLength("description", r.description(), DESCRIPTION_MAX)
                .throwIfInvalid();
    }

    @Bean
    SpecialtyExistsProvider specialtyExistsProvider() {
        return specialties::existsById;
    }
}