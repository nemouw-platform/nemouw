package com.nemouw.specialist.profile;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.util.Validator;
import com.nemouw.media.api.PublicMediaUrls;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistRejectionRequest;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistResponse;
import com.nemouw.user.api.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.strip;
import static com.nemouw.common.util.ValidationLimits.DESCRIPTION_MAX;

/** Staff actions on a specialist. Authorization is on the controller; these methods assume it has passed. */
@Service
class SpecialistReviewService {
    private static final Logger log = LoggerFactory.getLogger(SpecialistReviewService.class);

    private final SpecialistRepository specialists;
    private final AccountService accounts;
    private final PublicMediaUrls publicMediaUrls;

    SpecialistReviewService(SpecialistRepository specialists, AccountService accounts,
                            PublicMediaUrls publicMediaUrls) {
        this.specialists = specialists;
        this.accounts = accounts;
        this.publicMediaUrls = publicMediaUrls;
    }

    @Transactional(readOnly = true)
    SpecialistResponse get(UUID specialistId) {
        return respond(find(specialistId));
    }

    @Transactional
    SpecialistResponse verify(UUID specialistId, UUID reviewerId) {
        SpecialistRow current = lock(specialistId);
        if (current.verificationStatus() == SpecialistVerificationStatus.ACTIVE) return respond(current);
        specialists.markVerified(specialistId, reviewerId);
        log.info("SPECIALIST_VERIFIED specialist={} reviewer={}", specialistId, reviewerId);
        return respond(find(specialistId));
    }

    @Transactional
    SpecialistResponse reject(UUID specialistId, UUID reviewerId, SpecialistRejectionRequest req) {
        new Validator()
                .required("rejection_reason", req.rejectionReason())
                .maxLength("rejection_reason", req.rejectionReason(), DESCRIPTION_MAX)
                .throwIfInvalid();
        lock(specialistId);
        specialists.markRejected(specialistId, reviewerId, strip(req.rejectionReason()));
        log.info("SPECIALIST_REJECTED specialist={} reviewer={}", specialistId, reviewerId);
        return respond(find(specialistId));
    }

    @Transactional
    SpecialistResponse suspend(UUID specialistId, UUID actorId) {
        accounts.suspend(find(specialistId).userId());
        log.info("SPECIALIST_SUSPENDED specialist={} actor={}", specialistId, actorId);
        return respond(find(specialistId));
    }

    @Transactional
    SpecialistResponse unsuspend(UUID specialistId, UUID actorId) {
        accounts.unsuspend(find(specialistId).userId());
        log.info("SPECIALIST_UNSUSPENDED specialist={} actor={}", specialistId, actorId);
        return respond(find(specialistId));
    }

    private SpecialistRow find(UUID specialistId) {
        return specialists.findById(specialistId).orElseThrow(SpecialistReviewService::notFound);
    }

    private SpecialistRow lock(UUID specialistId) {
        return specialists.lockById(specialistId).orElseThrow(SpecialistReviewService::notFound);
    }

    private SpecialistResponse respond(SpecialistRow row) {
        return SpecialistResponse.from(row, publicMediaUrls);
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }
}