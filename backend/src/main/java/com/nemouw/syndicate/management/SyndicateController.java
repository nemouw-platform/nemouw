package com.nemouw.syndicate.management;

import com.nemouw.security.AdminOnly;
import com.nemouw.syndicate.access.AdminOrSyndicateOwner;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateCreateRequest;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateResponse;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateSummary;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/syndicates")
public class SyndicateController {
    private final SyndicateService service;

    SyndicateController(SyndicateService service) {
        this.service = service;
    }

    @GetMapping
    List<SyndicateSummary> list(@RequestParam(name = "specialty_id", required = false) UUID specialtyId,
                                @RequestParam(name = "country_id", required = false) UUID countryId) {
        return service.list(specialtyId, countryId);
    }

    @GetMapping("/{syndicateId}")
    SyndicateSummary get(@PathVariable UUID syndicateId) {
        return service.get(syndicateId);
    }

    @PostMapping
    @AdminOnly
    public ResponseEntity<SyndicateResponse> create(@RequestBody SyndicateCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{syndicateId}")
    @AdminOrSyndicateOwner
    public SyndicateResponse update(@PathVariable UUID syndicateId, @RequestBody SyndicateUpdateRequest req) {
        return service.update(syndicateId, req);
    }
}