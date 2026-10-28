package com.nemouw.syndicate.staff;

import com.nemouw.user.api.AccountService;
import com.nemouw.user.api.InviteService;
import com.nemouw.user.api.InviteService.Invite;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.util.Validator;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffEmailRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffInviteRequest;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class SyndicateStaffService {
    private record Created(SyndicateStaffResponse response, Invite invite) {
    }

    private final SyndicateStaffRepository staff;
    private final AccountService accounts;
    private final InviteService invites;
    private final SyndicateStaffAssembler assembler;
    private final TransactionTemplate tx;

    SyndicateStaffService(SyndicateStaffRepository staff,
                          AccountService accounts, InviteService invites,
                          SyndicateStaffAssembler assembler, TransactionTemplate tx) {
        this.staff = staff;
        this.accounts = accounts;
        this.invites = invites;
        this.assembler = assembler;
        this.tx = tx;
    }

    @Transactional(readOnly = true)
    public List<SyndicateStaffResponse> list(UUID syndicateId) {
        requireSyndicate(syndicateId);
        return assembler.toResponses(staff.findBySyndicate(syndicateId));
    }

    public SyndicateStaffResponse get(UUID syndicateId, UUID staffId) {
        return assembler.toResponse(findRow(syndicateId, staffId));
    }

    public SyndicateStaffResponse invite(UUID syndicateId, SyndicateStaffInviteRequest req) {
        new Validator()
                .required("first_name", req.firstName())
                .required("last_name", req.lastName())
                .requiredEmail("email", req.email())
                .required("title", req.title())
                .maxLength("first_name", req.firstName(), TEXT_MAX)
                .maxLength("last_name", req.lastName(), TEXT_MAX)
                .maxLength("title", req.title(), TEXT_MAX)
                .throwIfInvalid();

        Created created = tx.execute(status -> {
            requireSyndicate(syndicateId);
            Invite invite = invites.createPendingAccount(
                    UserRole.SYNDICATE_STAFF, req.firstName(), req.lastName(), req.email());
            UUID memberId = staff.insertMember(syndicateId, invite.userId(), req.title().trim());
            return new Created(assembler.toResponse(findRow(syndicateId, memberId)), invite);
        });
        invites.sendInvite(created.invite(), LocaleCode.EN);   // only after the commit
        return created.response();
    }

    @Transactional
    SyndicateStaffResponse updateTitle(UUID syndicateId, UUID staffId, SyndicateStaffUpdateRequest req) {
        new Validator()
                .required("title", req.title())
                .maxLength("title", req.title(), TEXT_MAX)
                .throwIfInvalid();
        if (!staff.updateTitle(staffId, syndicateId, req.title().trim())) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return assembler.toResponse(findRow(syndicateId, staffId));
    }

    /**
     * Fixes a mistyped invite address (or resends the invite) for a staff member who never activated.
     * Refused once the account is active. Ownership is not involved, so the owner is repaired the same way.
     */
    SyndicateStaffResponse changePendingEmail(UUID syndicateId, UUID staffId,
                                              SyndicateStaffEmailRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();

        Created created = tx.execute(status -> {
            SyndicateStaffRow member = findRow(syndicateId, staffId);
            Invite invite = invites.changePendingEmail(member.userId(), req.email());
            return new Created(assembler.toResponse(findRow(syndicateId, staffId)), invite);
        });
        invites.sendInvite(created.invite(), LocaleCode.EN);   // only after the commit
        return created.response();
    }

    @Transactional
    void remove(UUID syndicateId, UUID staffId) {
        SyndicateStaffRow member = findRow(syndicateId, staffId);
        if (member.isOwner()) throw new ApiException(ErrorCode.OWNER_CANNOT_BE_REMOVED);
        staff.delete(member.id());
        accounts.deleteAccount(member.userId(), UserRole.SYNDICATE_STAFF);
    }

    private void requireSyndicate(UUID syndicateId) {
        if (!staff.syndicateExists(syndicateId)) throw new ApiException(ErrorCode.NOT_FOUND);
    }

    /** Both ids must match: a staff id from another syndicate is a 404, never a cross-syndicate edit. */
    private SyndicateStaffRow findRow(UUID syndicateId, UUID staffId) {
        return staff.findInSyndicate(staffId, syndicateId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }
}