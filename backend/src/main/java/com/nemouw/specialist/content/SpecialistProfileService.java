package com.nemouw.specialist.content;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.content.SpecialistProfileDtos.SpecialistProfileResponse;
import com.nemouw.specialist.content.SpecialistProfileDtos.SpecialistProfileWriteRequest;
import com.nemouw.specialist.profile.SpecialistLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.BIO_MAX;
import static com.nemouw.common.util.ValidationLimits.NAME_MAX_LENGTH;
import static com.nemouw.common.util.ValidationLimits.HEADLINE_MAX;

@Service
class SpecialistProfileService {
    private final SpecialistProfileRepository profiles;
    private final SpecialistLookup specialists;

    SpecialistProfileService(SpecialistProfileRepository profiles, SpecialistLookup specialists) {
        this.profiles = profiles;
        this.specialists = specialists;
    }

    @Transactional(readOnly = true)
    List<SpecialistProfileResponse> list(UUID userId) {
        return profiles.findAll(specialists.requireIdOf(userId)).stream().map(SpecialistProfileResponse::from).toList();
    }

    @Transactional(readOnly = true)
    SpecialistProfileResponse get(UUID userId, String locale) {
        UUID specialistId = specialists.requireIdOf(userId);
        return profiles.find(specialistId, parseLocale(locale).code())
                .map(SpecialistProfileResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "No content exists for this locale yet"));
    }

    @Transactional
    SpecialistProfileResponse put(UUID userId, String locale, SpecialistProfileWriteRequest req) {
        UUID specialistId = specialists.requireIdOf(userId);
        LocaleCode code = parseLocale(locale);
        new Validator().maxLength("title", req.title(), NAME_MAX_LENGTH).maxLength("headline", req.headline(), HEADLINE_MAX).maxLength("bio", req.bio(), BIO_MAX).throwIfInvalid();
        return SpecialistProfileResponse.from(profiles.upsert(specialistId, code.code(), blankToNull(req.title()), blankToNull(req.headline()), blankToNull(req.bio())));
    }

    @Transactional
    void delete(UUID userId, String locale) {
        UUID specialistId = specialists.requireIdOf(userId);
        if (!profiles.delete(specialistId, parseLocale(locale).code())) throw new ApiException(ErrorCode.NOT_FOUND);
    }

    private static LocaleCode parseLocale(String locale) {
        return LocaleCode.tryFrom(locale).orElseThrow(() -> new ValidationException(List.of(FieldErrors.invalidFormat("locale", "must be one of en, ar, fr"))));
    }
}