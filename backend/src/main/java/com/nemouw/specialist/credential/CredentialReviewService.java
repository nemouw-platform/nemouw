package com.nemouw.specialist.credential;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.credential.CredentialDtos.CredentialResponse;
import com.nemouw.specialist.credential.CredentialDtos.DocumentLinkResponse;
import com.nemouw.specialist.credential.CredentialDtos.RejectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.nemouw.common.util.ValidationLimits.DESCRIPTION_MAX;

/** Reviewer actions. Authorization is on the controller; these methods assume it has passed. */
@Service
class CredentialReviewService {
    private static final Logger log = LoggerFactory.getLogger(CredentialReviewService.class);

    private final CredentialRepository credentials;
    private final CredentialDocumentLinks links;

    CredentialReviewService(CredentialRepository credentials, CredentialDocumentLinks links) {
        this.credentials = credentials;
        this.links = links;
    }

    @Transactional(readOnly = true)
    List<CredentialResponse> queue(UUID syndicateId, String statusText) {
        CredentialVerificationStatus status = null;
        if (statusText != null) {
            status = CredentialVerificationStatus.tryFrom(statusText).orElseThrow(() -> Errors.invalidField(
                    "verification_status", "must be one of " + CredentialVerificationStatus.allCodes()));
        }
        return credentials.findBySyndicate(syndicateId, status).stream().map(CredentialResponse::from).toList();
    }

    @Transactional
    CredentialResponse verify(UUID credentialId, UUID reviewerId) {
        locked(credentialId);
        credentials.markVerified(credentialId, reviewerId);
        log.info("CREDENTIAL_VERIFIED credential={} reviewer={}", credentialId, reviewerId);
        return respond(credentialId);
    }

    @Transactional
    CredentialResponse reject(UUID credentialId, UUID reviewerId, RejectionRequest req) {
        new Validator()
                .required("rejection_reason", req.rejectionReason())
                .maxLength("rejection_reason", req.rejectionReason(), DESCRIPTION_MAX)
                .throwIfInvalid();
        locked(credentialId);
        credentials.markRejected(credentialId, reviewerId, req.rejectionReason().trim());
        log.info("CREDENTIAL_REJECTED credential={} reviewer={}", credentialId, reviewerId);
        return respond(credentialId);
    }

    DocumentLinkResponse documentLink(UUID credentialId) {
        return links.issue(credentials.find(credentialId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND)));
    }

    private void locked(UUID credentialId) {
        credentials.lock(credentialId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private CredentialResponse respond(UUID credentialId) {
        return CredentialResponse.from(credentials.find(credentialId).orElseThrow());
    }
}