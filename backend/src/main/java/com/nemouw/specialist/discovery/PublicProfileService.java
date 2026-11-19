package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.Codes;
import com.nemouw.common.enums.Currency;
import com.nemouw.common.enums.Gender;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.time.PlatformTime;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.media.api.PublicMediaUrls;
import com.nemouw.specialist.discovery.DiscoveryDtos.PublicCredential;
import com.nemouw.specialist.discovery.DiscoveryDtos.SpecialistPublicProfile;
import com.nemouw.specialist.discovery.DiscoveryDtos.SyndicateRef;
import com.nemouw.specialist.discovery.PublicProfileRepository.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
class PublicProfileService {
    private final PublicProfileRepository repository;
    private final PublicMediaUrls mediaUrls;
    private final PlatformTime clock;

    PublicProfileService(PublicProfileRepository repository, PublicMediaUrls mediaUrls, PlatformTime clock) {
        this.repository = repository;
        this.mediaUrls = mediaUrls;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    SpecialistPublicProfile get(String username, LocaleCode requested) {
        Profile p = repository.find(username.trim().toLowerCase(Locale.ROOT), requested.code())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        SyndicateRef syndicate = new SyndicateRef(p.syndicateId(), p.syndicateName());
        List<PublicCredential> credentials = repository.credentials(p.id(), clock.now().toLocalDate()).stream()
                .map(c -> new PublicCredential(c.credentialType(), c.title(), c.issuingOrganization(),
                        c.issueDate(), c.expiryDate(), c.verifiedAt().toInstant(), syndicate))
                .toList();

        return new SpecialistPublicProfile(p.id(), p.username(), p.firstName(), p.lastName(),
                p.contentLocale() == null ? requested : LocaleCode.orDefault(p.contentLocale()),
                p.title(), p.headline(), p.bio(), p.specialtyId(), Codes.orNull(Gender.class, p.gender()),
                p.yearsOfExperience(), Codes.orNull(LocaleCode.class, p.primaryLanguage()), p.sessionPriceMin(),
                p.sessionPriceMax(), Codes.fromCode(Currency.class, p.priceCurrency()),
                p.whatsappNumber(), p.minAgeTreated(), p.maxAgeTreated(),
                mediaUrls.urlFor(p.profilePictureKey()), mediaUrls.urlFor(p.coverImageKey()),
                repository.conditionIds(p.id()), repository.locations(p.id()),
                repository.availabilities(p.id()), credentials);
    }
}
