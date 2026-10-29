package com.nemouw.specialist.template;

import com.nemouw.specialist.profile.CurrentSpecialistId;
import com.nemouw.specialist.template.SpecialistTemplateDtos.SpecialistTemplateResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me/template")
public class SpecialistTemplateController {
    private final SpecialistTemplateService service;

    SpecialistTemplateController(SpecialistTemplateService service) {
        this.service = service;
    }

    @GetMapping
    SpecialistTemplateResponse get(@CurrentSpecialistId UUID specialistId) {
        return service.get(specialistId);
    }

    @PutMapping
    SpecialistTemplateResponse update(@CurrentSpecialistId UUID specialistId,
                                      @RequestBody Map<String, Object> body) {
        return service.update(specialistId, body);
    }
}