package com.nemouw.syndicate.staff;

import com.nemouw.security.AdminOnly;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateOwnerChangeRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/syndicates/{syndicateId}/owner")
@AdminOnly
public class SyndicateOwnerController {
    private final SyndicateOwnerService service;

    SyndicateOwnerController(SyndicateOwnerService service) {
        this.service = service;
    }

    @PutMapping
    SyndicateStaffResponse changeOwner(@PathVariable UUID syndicateId,
                                       @RequestBody SyndicateOwnerChangeRequest req) {
        return service.changeOwner(syndicateId, req);
    }
}