package com.nemouw.specialist.treatedcondition;

import com.nemouw.specialist.profile.CurrentSpecialistId;
import com.nemouw.specialist.treatedcondition.TreatedConditionDtos.TreatedConditionsWriteRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me/conditions")
public class TreatedConditionController {
    private final TreatedConditionService service;

    TreatedConditionController(TreatedConditionService service) {
        this.service = service;
    }

    @GetMapping
    List<UUID> list(@CurrentSpecialistId UUID specialistId) {
        return service.list(specialistId);
    }

    @PutMapping
    List<UUID> replace(@CurrentSpecialistId UUID specialistId, @RequestBody TreatedConditionsWriteRequest req) {
        return service.replace(specialistId, req);
    }
}