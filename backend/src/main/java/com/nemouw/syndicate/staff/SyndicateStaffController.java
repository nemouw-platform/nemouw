package com.nemouw.syndicate.staff;

import com.nemouw.syndicate.access.AdminOrSyndicateOwner;
import com.nemouw.syndicate.access.AdminOrSyndicateStaff;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/syndicates/{syndicateId}/staff")
@AdminOrSyndicateOwner
public class SyndicateStaffController {
    private final SyndicateStaffService service;

    SyndicateStaffController(SyndicateStaffService service) {
        this.service = service;
    }

    @GetMapping
    @AdminOrSyndicateStaff
    public List<SyndicateStaffResponse> list(@PathVariable UUID syndicateId) {
        return service.list(syndicateId);
    }

    @PostMapping
    ResponseEntity<SyndicateStaffResponse> invite(@PathVariable UUID syndicateId,
                                                  @RequestBody SyndicateStaffInviteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(syndicateId, req));
    }

    @GetMapping("/{staffId}")
    @AdminOrSyndicateStaff
    public SyndicateStaffResponse get(@PathVariable UUID syndicateId, @PathVariable UUID staffId) {
        return service.get(syndicateId, staffId);
    }

    @PatchMapping("/{staffId}")
    SyndicateStaffResponse update(@PathVariable UUID syndicateId, @PathVariable UUID staffId,
                                  @RequestBody SyndicateStaffUpdateRequest req) {
        return service.updateTitle(syndicateId, staffId, req);
    }

    @PutMapping("/{staffId}/email")
    SyndicateStaffResponse changePendingEmail(@PathVariable UUID syndicateId, @PathVariable UUID staffId,
                                              @RequestBody SyndicateStaffEmailRequest req) {
        return service.changePendingEmail(syndicateId, staffId, req);
    }

    @DeleteMapping("/{staffId}")
    ResponseEntity<Void> remove(@PathVariable UUID syndicateId, @PathVariable UUID staffId) {
        service.remove(syndicateId, staffId);
        return ResponseEntity.noContent().build();
    }
}