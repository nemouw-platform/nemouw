package com.nemouw.specialist.credential;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.media.api.MediaStore;
import com.nemouw.specialist.credential.CredentialDtos.DocumentLinkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** The one place a credential document link is issued, so every issue is logged. The caller authorizes first. */
@Component
class CredentialDocumentLinks {
    private static final Logger log = LoggerFactory.getLogger(CredentialDocumentLinks.class);

    private final MediaStore media;

    CredentialDocumentLinks(MediaStore media) {
        this.media = media;
    }

    DocumentLinkResponse issue(CredentialRow credential) {
        if (credential.documentId() == null) throw new ApiException(ErrorCode.NOT_FOUND);
        log.info("credential_document_link credential={}", credential.id());
        return DocumentLinkResponse.from(media.presign(credential.documentId()));
    }
}