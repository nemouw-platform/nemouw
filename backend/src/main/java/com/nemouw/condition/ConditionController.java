package com.nemouw.condition;

import com.nemouw.condition.ConditionDtos.ConditionResponse;
import com.nemouw.condition.ConditionDtos.ConditionWriteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conditions")
public class ConditionController {
    private final ConditionService service;

    ConditionController(ConditionService service) {
        this.service = service;
    }

    @GetMapping
    List<ConditionResponse> list(@RequestParam(name = "is_active", required = false) Boolean isActive,
                                 @RequestParam(required = false) String search) {
        return service.list(isActive, search);
    }

    @PostMapping
    ResponseEntity<ConditionResponse> create(@RequestBody ConditionWriteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{conditionId}")
    ConditionResponse update(@PathVariable UUID conditionId, @RequestBody ConditionWriteRequest req) {
        return service.update(conditionId, req);
    }

    @DeleteMapping("/{conditionId}")
    ResponseEntity<Void> delete(@PathVariable UUID conditionId) {
        service.delete(conditionId);
        return ResponseEntity.noContent().build();
    }
}