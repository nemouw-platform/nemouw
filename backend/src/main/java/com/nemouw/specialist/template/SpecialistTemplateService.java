package com.nemouw.specialist.template;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.specialist.template.SpecialistTemplateDtos.SpecialistTemplateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
class SpecialistTemplateService {
    private static final String PRIMARY_COLOR = "primary_color";
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final SpecialistTemplateRepository templates;

    SpecialistTemplateService(SpecialistTemplateRepository templates) {
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    SpecialistTemplateResponse get(UUID specialistId) {
        return templates.find(specialistId)
                .map(SpecialistTemplateResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    /** An omitted key is left unchanged; an explicit null clears it. */
    @Transactional
    SpecialistTemplateResponse update(UUID specialistId, Map<String, Object> body) {
        boolean colorSent = body.containsKey(PRIMARY_COLOR);
        String color = colorSent ? parseColor(body.get(PRIMARY_COLOR)) : null;
        return SpecialistTemplateResponse.from(templates.upsert(specialistId, colorSent, color));
    }

    private static String parseColor(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String s && HEX_COLOR.matcher(s.trim()).matches()) {
            return s.trim().toLowerCase(Locale.ROOT);
        }
        throw Errors.invalidField(PRIMARY_COLOR, "must be a hex color such as #1a73e8");
    }
}