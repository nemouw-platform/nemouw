package com.nemouw.syndicate.staff;

import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * What management may ask of staff: register the owner of a new syndicate, and read the current owner.
 */
@Component
public class SyndicateOwners {
    private final SyndicateStaffRepository staff;
    private final SyndicateStaffAssembler assembler;

    SyndicateOwners(SyndicateStaffRepository staff, SyndicateStaffAssembler assembler) {
        this.staff = staff;
        this.assembler = assembler;
    }

    public SyndicateStaffResponse addOwner(UUID syndicateId, UUID userId) {
        UUID staffId = staff.insertOwner(syndicateId, userId);
        return assembler.toResponse(staff.findInSyndicate(staffId, syndicateId).orElseThrow());
    }

    public Optional<SyndicateStaffResponse> findOwner(UUID syndicateId) {
        return staff.findOwner(syndicateId).map(assembler::toResponse);
    }
}