package com.nemouw.specialist.credential;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.media.api.MediaKind;
import com.nemouw.media.api.MediaStore;
import com.nemouw.media.api.StoredMedia;
import com.nemouw.media.api.UploadedObject;
import com.nemouw.specialist.credential.CredentialDtos.CredentialResponse;
import com.nemouw.specialist.credential.CredentialDtos.CredentialWriteRequest;
import com.nemouw.specialist.credential.CredentialDtos.DocumentLinkResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
class CredentialService {
    private final CredentialRepository credentials;
    private final CredentialDocumentLinks links;
    private final MediaStore media;
    private final TransactionTemplate tx;

    CredentialService(CredentialRepository credentials, CredentialDocumentLinks links, MediaStore media,
                      TransactionTemplate tx) {
        this.credentials = credentials;
        this.links = links;
        this.media = media;
        this.tx = tx;
    }

    @Transactional(readOnly = true)
    List<CredentialResponse> list(UUID specialistId) {
        return credentials.findBySpecialist(specialistId).stream().map(CredentialResponse::from).toList();
    }

    @Transactional(readOnly = true)
    CredentialResponse get(UUID specialistId, UUID id) {
        return CredentialResponse.from(owned(specialistId, id));
    }

    /** Metadata is validated before the upload, so a bad form never costs an S3 write. */
    CredentialResponse submit(UUID specialistId, CredentialWriteRequest req, MultipartFile document) {
        CredentialFields fields = CredentialFieldsValidator.validated(req);
        UploadedObject uploaded = media.upload(MediaKind.CREDENTIAL_DOCUMENT, document);
        UUID id = tx.execute(status -> {
            StoredMedia stored = media.register(uploaded);
            return credentials.insert(specialistId, fields, stored.id());
        });
        return CredentialResponse.from(owned(specialistId, id));
    }

    @Transactional
    CredentialResponse update(UUID specialistId, UUID id, CredentialWriteRequest req) {
        CredentialFields fields = CredentialFieldsValidator.validated(req);
        requireEditable(lockedOwned(specialistId, id));
        credentials.update(id, fields);
        return CredentialResponse.from(owned(specialistId, id));
    }

    /** Allowed in any status */
    @Transactional
    void withdraw(UUID specialistId, UUID id) {
        lockedOwned(specialistId, id);
        credentials.delete(id);
    }

    /** Checked once before the upload to fail fast, and again under the row lock to be correct. */
    CredentialResponse replaceDocument(UUID specialistId, UUID id, MultipartFile document) {
        requireEditable(owned(specialistId, id));
        UploadedObject uploaded = media.upload(MediaKind.CREDENTIAL_DOCUMENT, document);
        tx.executeWithoutResult(status -> {
            requireEditable(lockedOwned(specialistId, id));
            credentials.replaceDocument(id, media.register(uploaded).id());
        });
        return CredentialResponse.from(owned(specialistId, id));
    }

    DocumentLinkResponse documentLink(UUID specialistId, UUID id) {
        return links.issue(owned(specialistId, id));
    }

    private CredentialRow owned(UUID specialistId, UUID id) {
        return credentials.findOwned(id, specialistId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private CredentialRow lockedOwned(UUID specialistId, UUID id) {
        return credentials.lockOwned(id, specialistId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private static void requireEditable(CredentialRow row) {
        if (row.verificationStatus() == CredentialVerificationStatus.VERIFIED) throw Errors.credentialVerified();
    }
}