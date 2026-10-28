package com.nemouw.syndicate.staff;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.util.Validator;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateOwnerChangeRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Who owns a syndicate. The previous owner always stays on as regular staff. */
@Service
class SyndicateOwnerService {
    private static final Logger log = LoggerFactory.getLogger(SyndicateOwnerService.class);

    private final SyndicateStaffRepository staff;
    private final SyndicateStaffAssembler assembler;

    SyndicateOwnerService(SyndicateStaffRepository staff,
                          SyndicateStaffAssembler assembler) {
        this.staff = staff;
        this.assembler = assembler;
    }

    @Transactional
    SyndicateStaffResponse changeOwner(UUID syndicateId, SyndicateOwnerChangeRequest req) {
        new Validator().notNull("staff_id", req.staffId()).throwIfInvalid();

        if (!staff.syndicateExists(syndicateId)) throw new ApiException(ErrorCode.NOT_FOUND);
        SyndicateStaffRow next = staff.findInSyndicate(req.staffId(), syndicateId)   // must be staff of THIS syndicate
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (next.isOwner()) throw new ApiException(ErrorCode.ALREADY_OWNER);
        SyndicateStaffRow current = staff.findOwner(syndicateId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        staff.transferOwnership(current.id(), next.id());

        log.info("OWNER_CHANGED syndicate={} from={} to={}", syndicateId, current.userId(), next.userId());
        return assembler.toResponse(staff.findInSyndicate(next.id(), syndicateId).orElseThrow());
    }
}