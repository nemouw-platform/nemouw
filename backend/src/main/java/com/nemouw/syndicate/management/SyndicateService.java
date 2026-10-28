package com.nemouw.syndicate.management;

import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.util.TextNormalizer;
import com.nemouw.common.util.Validator;
import com.nemouw.country.CountryLookup;
import com.nemouw.specialty.SpecialtyLookup;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateCreateRequest;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateResponse;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateSummary;
import com.nemouw.syndicate.management.SyndicateDtos.SyndicateUpdateRequest;
import com.nemouw.syndicate.staff.SyndicateOwners;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class SyndicateService {
    private record Created(SyndicateResponse response, Invite invite) {
    }

    private final SyndicateRepository syndicates;
    private final CountryLookup countryLookup;
    private final SpecialtyLookup specialtyLookup;
    private final InviteService invites;
    private final SyndicateOwners owners;
    private final TransactionTemplate tx;

    SyndicateService(SyndicateRepository syndicates, CountryLookup countryLookup, SpecialtyLookup specialtyLookup,
                     InviteService invites, SyndicateOwners owners, TransactionTemplate tx) {
        this.syndicates = syndicates;
        this.countryLookup = countryLookup;
        this.specialtyLookup = specialtyLookup;
        this.invites = invites;
        this.owners = owners;
        this.tx = tx;
    }

    public List<SyndicateSummary> list(UUID specialtyId, UUID countryId) {
        return syndicates.findAll(specialtyId, countryId).stream().map(SyndicateSummary::from).toList();
    }

    public SyndicateSummary get(UUID id) {
        return SyndicateSummary.from(find(id));
    }

    SyndicateResponse create(SyndicateCreateRequest req) {
        validate(req);
        Created created = tx.execute(status -> createInTransaction(req));
        invites.sendInvite(created.invite(), LocaleCode.EN);   // only after the commit
        return created.response();
    }

    private Created createInTransaction(SyndicateCreateRequest r) {
        if (!specialtyLookup.existsById(r.specialtyId())) throw unknown("specialty_id", "unknown specialty");
        if (!countryLookup.existsById(r.countryId())) throw unknown("country_id", "unknown country");
        if (syndicates.existsByCountryAndSpecialty(r.countryId(), r.specialtyId())) {
            throw new ApiException(ErrorCode.SYNDICATE_EXISTS);
        }

        Invite invite = invites.createPendingAccount(
                UserRole.SYNDICATE_STAFF, r.ownerFirstName(), r.ownerLastName(), r.ownerEmail());

        Syndicate syndicate;
        try {
            syndicate = syndicates.insert(r.name().trim(), r.specialtyId(), r.countryId(),
                    TextNormalizer.normalizeEmail(r.contactEmail()), blankToNull(r.contactPhone()));
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorCode.SYNDICATE_EXISTS);   // lost a race with another create
        }

        SyndicateStaffResponse owner = owners.addOwner(syndicate.id(), invite.userId());
        return new Created(SyndicateResponse.from(syndicate, owner), invite);
    }

    @Transactional
    SyndicateResponse update(UUID id, SyndicateUpdateRequest req) {
        validate(req);
        Syndicate current = find(id);

        String name = req.name() != null ? req.name().trim() : current.name();
        String email = req.contactEmail() != null
                ? TextNormalizer.normalizeEmail(req.contactEmail()) : current.contactEmail();
        String phone = req.contactPhone() != null ? blankToNull(req.contactPhone()) : current.contactPhone();

        Syndicate updated = syndicates.update(id, name, email, phone);
        SyndicateStaffResponse owner = owners.findOwner(id).orElse(null);
        return SyndicateResponse.from(updated, owner);
    }

    private Syndicate find(UUID id) {
        return syndicates.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private static ValidationException unknown(String field, String message) {
        return new ValidationException(List.of(FieldErrors.invalidFormat(field, message)));
    }

    private static void validate(SyndicateCreateRequest r) {
        new Validator()
                .required("name", r.name())
                .notNull("specialty_id", r.specialtyId())
                .notNull("country_id", r.countryId())
                .requiredEmail("contact_email", r.contactEmail())
                .required("owner_first_name", r.ownerFirstName())
                .required("owner_last_name", r.ownerLastName())
                .requiredEmail("owner_email", r.ownerEmail())
                .maxLength("name", r.name(), TEXT_MAX)
                .maxLength("contact_phone", r.contactPhone(), TEXT_MAX)
                .maxLength("owner_first_name", r.ownerFirstName(), TEXT_MAX)
                .maxLength("owner_last_name", r.ownerLastName(), TEXT_MAX)
                .throwIfInvalid();
    }

    private static void validate(SyndicateUpdateRequest r) {
        new Validator()
                .requiredForWrite(false, "name", r.name())
                .requiredForWrite(false, "contact_email", r.contactEmail())
                .maxLength("name", r.name(), TEXT_MAX)
                .email("contact_email", r.contactEmail())
                .maxLength("contact_phone", r.contactPhone(), TEXT_MAX)
                .throwIfInvalid();
    }
}