package com.nemouw.specialist.profile;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistOnboardingRequest;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class SpecialistService {
    private final SpecialistRepository specialists;
    private final MediaUrls mediaUrls;

    SpecialistService(SpecialistRepository specialists, MediaUrls mediaUrls) {
        this.specialists = specialists;
        this.mediaUrls = mediaUrls;
    }

    @Transactional(readOnly = true)
    SpecialistResponse getMine(UUID userId) {
        return respond(specialists.findByUserId(userId).orElseThrow(Errors::specialistNotFound));
    }

    @Transactional
    SpecialistResponse onboard(UUID userId, SpecialistOnboardingRequest req) {
        new Validator()
                .required("username", req.username())
                .username("username", req.username())
                .check(req.username() == null || !UsernamePolicy.isReserved(req.username()),
                        FieldErrors.invalidFormat("username", "is not available"))
                .notNull("syndicate_id", req.syndicateId())
                .required("syndicate_member_number", req.syndicateMemberNumber())
                .maxLength("syndicate_member_number", req.syndicateMemberNumber(), TEXT_MAX)
                .throwIfInvalid();

        if (specialists.findByUserId(userId).isPresent()) throw new ApiException(ErrorCode.PROFILE_ALREADY_EXISTS);
        if (!specialists.syndicateExists(req.syndicateId())) throw unknown("syndicate_id", "unknown syndicate");

        String username = normalizeUsername(req.username());
        String memberNumber = req.syndicateMemberNumber().trim();
        if (specialists.usernameTaken(username, null)) throw new ApiException(ErrorCode.USERNAME_TAKEN);
        if (specialists.memberNumberTaken(req.syndicateId(), memberNumber, null)) {
            throw new ApiException(ErrorCode.MEMBER_NUMBER_TAKEN);
        }

        UUID id;
        try {
            id = specialists.insert(userId, username, req.syndicateId(), memberNumber);
        } catch (DuplicateKeyException e) {
            throw conflict(e);
        }
        return respond(specialists.findById(id).orElseThrow());
    }

    /**
     * Partial update. A key that is absent is left alone; a key sent as null clears the field
     * (or is rejected for the mandatory ones).
     */
    @Transactional
    SpecialistResponse update(UUID userId, Map<String, Object> body) {
        SpecialistRow cur = specialists.findByUserId(userId).orElseThrow(Errors::onboardingRequired);
        Validator v = new Validator();
        Map<String, Object> changes = new LinkedHashMap<>();

        // ---- 1. parse and validate ----
        String username = cur.username();
        if (body.containsKey("username")) {
            String raw = requiredText(body, "username", v);
            if (raw != null) {
                v.username("username", raw)
                        .check(!UsernamePolicy.isReserved(raw),
                                FieldErrors.invalidFormat("username", "is not available"));
                username = normalizeUsername(raw);
            }
        }

        String memberNumber = cur.syndicateMemberNumber();
        if (body.containsKey("syndicate_member_number")) {
            String raw = requiredText(body, "syndicate_member_number", v);
            if (raw != null) {
                v.maxLength("syndicate_member_number", raw, TEXT_MAX);
                memberNumber = raw;
            }
        }

        UUID syndicateId = cur.syndicateId();
        if (body.containsKey("syndicate_id")) {
            if (body.get("syndicate_id") == null) {
                v.required("syndicate_id", null);
            } else {
                UUID sent = uuid(body, "syndicate_id", v);
                if (sent != null) syndicateId = sent;
            }
        }
        boolean syndicateChanged = !syndicateId.equals(cur.syndicateId());
        // A member number belongs to one syndicate, so a new syndicate needs a new number in the same request
        if (syndicateChanged && !body.containsKey("syndicate_member_number")) {
            v.required("syndicate_member_number", null);
        }

        for (String key : List.of("gender", "whatsapp_number", "primary_language")) {
            if (body.containsKey(key)) changes.put(key, optionalText(body, key, v));
        }

        if (body.containsKey("years_of_experience")) {
            changes.put("years_of_experience", optionalInt(body, "years_of_experience", v));
        }
        Integer min = cur.minAgeTreated();
        Integer max = cur.maxAgeTreated();
        if (body.containsKey("min_age_treated")) {
            min = optionalInt(body, "min_age_treated", v);
            changes.put("min_age_treated", min);
        }
        if (body.containsKey("max_age_treated")) {
            max = optionalInt(body, "max_age_treated", v);
            changes.put("max_age_treated", max);
        }
        if (min != null && max != null && min > max) {
            v.check(false, FieldErrors.invalidFormat("min_age_treated",
                    "min_age_treated must be less than or equal to max_age_treated"));
        }

        UUID picture = cur.profilePictureId();
        UUID cover = cur.coverImageId();
        if (body.containsKey("profile_picture_id")) {
            picture = optionalUuid(body, "profile_picture_id", v);
            changes.put("profile_picture_id", picture);
        }
        if (body.containsKey("cover_image_id")) {
            cover = optionalUuid(body, "cover_image_id", v);
            changes.put("cover_image_id", cover);
        }
        v.check(picture == null || !picture.equals(cover),
                FieldErrors.invalidFormat("cover_image_id", "must differ from profile_picture_id"));
        v.throwIfInvalid();

        if (!username.equals(cur.username())) {
            if (specialists.usernameTaken(username, cur.id())) throw new ApiException(ErrorCode.USERNAME_TAKEN);
            changes.put("username", username);
        }
        if (syndicateChanged) {
            if (cur.verificationStatus() == SpecialistVerificationStatus.ACTIVE) {
                throw new ApiException(ErrorCode.SYNDICATE_LOCKED);
            }
            if (!specialists.syndicateExists(syndicateId)) throw unknown("syndicate_id", "unknown syndicate");
            changes.put("syndicate_id", syndicateId);
        }
        if (syndicateChanged || !memberNumber.equals(cur.syndicateMemberNumber())) {
            if (specialists.memberNumberTaken(syndicateId, memberNumber, cur.id())) {
                throw new ApiException(ErrorCode.MEMBER_NUMBER_TAKEN);
            }
            changes.put("syndicate_member_number", memberNumber);
        }
        requireUsableMedia("profile_picture_id", picture, cur.profilePictureId(), cur.id());
        requireUsableMedia("cover_image_id", cover, cur.coverImageId(), cur.id());

        try {
            specialists.update(cur.id(), changes);
        } catch (DuplicateKeyException e) {
            throw conflict(e);
        } catch (DataIntegrityViolationException e) {
            throw integrity(e);
        }
        return respond(specialists.findById(cur.id()).orElseThrow());
    }

    private SpecialistResponse respond(SpecialistRow row) {
        return SpecialistResponse.from(row, mediaUrls);
    }

    private void requireUsableMedia(String field, UUID id, UUID currentId, UUID specialistId) {
        if (id != null && !id.equals(currentId) && !specialists.mediaAvailable(id, specialistId)) {
            throw unknown(field, "unknown media or already in use");
        }
    }

    private static String normalizeUsername(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static ValidationException unknown(String field, String message) {
        return new ValidationException(List.of(FieldErrors.invalidFormat(field, message)));
    }

    /** Maps the unique index that fired to the contract's error. Anything else is a real bug: rethrow. */
    private static RuntimeException conflict(DuplicateKeyException e) {
        String m = String.valueOf(e.getMostSpecificCause().getMessage());
        if (m.contains("uq_specialists_syndicate_member")) return new ApiException(ErrorCode.MEMBER_NUMBER_TAKEN);
        if (m.contains("specialists_user_id_key")) return new ApiException(ErrorCode.PROFILE_ALREADY_EXISTS);
        if (m.contains("specialists_username_key") || m.contains("uq_user_identifiers_type_identifier")) {
            return new ApiException(ErrorCode.USERNAME_TAKEN);
        }
        if (m.contains("uq_specialists_profile_picture")) return unknown("profile_picture_id", "already in use");
        if (m.contains("uq_specialists_cover_image")) return unknown("cover_image_id", "already in use");
        return e;
    }

    private static RuntimeException integrity(DataIntegrityViolationException e) {
        String m = String.valueOf(e.getMostSpecificCause().getMessage());
        if (m.contains("syndicate_id cannot change")) return new ApiException(ErrorCode.SYNDICATE_LOCKED);   // verified between our read and write
        if (m.contains("ck_specialists_age_range")) {
            return unknown("min_age_treated", "min_age_treated must be less than or equal to max_age_treated");
        }
        if (m.contains("ck_specialists_picture_cover_differ")) {
            return unknown("cover_image_id", "must differ from profile_picture_id");
        }
        return e;
    }

    /** Mandatory when present: null, blank and non-string are all errors. Returns the trimmed text or null. */
    private static String requiredText(Map<String, Object> b, String key, Validator v) {
        Object o = b.get(key);
        if (o == null) {
            v.required(key, null);
            return null;
        }
        if (!(o instanceof String s)) {
            v.check(false, FieldErrors.invalidFormat(key, "must be a string"));
            return null;
        }
        v.required(key, s);
        return s.isBlank() ? null : s.trim();
    }

    /** Nullable text: null or blank clears the field. */
    private static String optionalText(Map<String, Object> b, String key, Validator v) {
        Object o = b.get(key);
        if (o == null) return null;
        if (!(o instanceof String s)) {
            v.check(false, FieldErrors.invalidFormat(key, "must be a string"));
            return null;
        }
        v.maxLength(key, s, TEXT_MAX);
        return blankToNull(s);
    }

    private static Integer optionalInt(Map<String, Object> b, String key, Validator v) {
        Object o = b.get(key);
        if (o == null) return null;
        if (o instanceof Integer i) {
            v.check(i >= 0, FieldErrors.invalidFormat(key, "must be 0 or greater"));
            return i;
        }
        v.check(false, FieldErrors.invalidFormat(key, "must be an integer"));
        return null;
    }

    private static UUID uuid(Map<String, Object> b, String key, Validator v) {
        if (b.get(key) instanceof String s) {
            try {
                return UUID.fromString(s.trim());
            } catch (IllegalArgumentException ignored) {
                // falls through to the error below
            }
        }
        v.check(false, FieldErrors.invalidFormat(key, "must be a valid UUID"));
        return null;
    }

    private static UUID optionalUuid(Map<String, Object> b, String key, Validator v) {
        return b.get(key) == null ? null : uuid(b, key, v);
    }
}