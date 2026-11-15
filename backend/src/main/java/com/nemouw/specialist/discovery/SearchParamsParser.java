package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.common.query.QueryParser;
import com.nemouw.common.query.QuerySpec;
import com.nemouw.specialist.availability.ClockTime;
import com.nemouw.specialist.availability.WeekDay;
import com.nemouw.specialist.discovery.SearchParams.Near;
import com.nemouw.specialist.discovery.SearchParams.Availability;
import org.springframework.util.MultiValueMap;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

/** Validates query parameters and turns them into {@link SearchParams}; reports every bad parameter at once. */
final class SearchParamsParser {
    static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_VALUES = 10;
    private static final int MAX_WORDS = 6;
    private static final Pattern NOT_WORD = Pattern.compile("[^\\p{L}\\p{N}\\p{M}]+");
    private static final Set<String> FILLER = Set.of(
            "dr", "doctor", "docteur", "dre", "prof", "professor", "mr", "mrs", "ms", "mme",
            "the", "and", "for", "with", "near", "in", "at", "of", "de", "du", "la", "le", "les", "des",
            "pour", "avec", "et", "دكتور", "الدكتور", "دكتورة", "في", "عند");

    private final MultiValueMap<String, String> params;
    private final Validator v = new Validator();

    private SearchParamsParser(MultiValueMap<String, String> params) {
        this.params = params;
    }

    static SearchParams parse(MultiValueMap<String, String> params) {
        return new SearchParamsParser(params).run();
    }

    private SearchParams run() {
        QuerySpec query = QueryParser.parse(SearchFields.FIELDS, params, v, MAX_PAGE_SIZE);
        Near near = near();
        if (near == null && query.sorts().stream().anyMatch(s -> s.key().equals(SearchParams.DISTANCE))) {
            fail("sort", "distance needs lat and lng");
        }
        String locale = text("locale");
        Optional<LocaleCode> parsedLocale = locale == null ? Optional.of(LocaleCode.EN) : LocaleCode.tryFrom(locale);
        if (parsedLocale.isEmpty()) fail("locale", "must be one of en, ar, fr");

        SearchParams result = new SearchParams(
                words(text("search"), true), words(text("location"), false), uuid("country_id"), near,
                uuids("condition_ids"), integer("patient_age", 0, 120), availability(),
                query, parsedLocale.orElse(LocaleCode.EN));
        v.throwIfInvalid();
        return result;
    }

    private Near near() {
        Double lat = decimal("lat", -90, 90);
        Double lng = decimal("lng", -180, 180);
        Double radius = decimal("radius_km", 0.1, 200);
        boolean hasLat = params.getFirst("lat") != null;
        boolean hasLng = params.getFirst("lng") != null;
        if (hasLat != hasLng) {
            fail(hasLat ? "lng" : "lat", "lat and lng go together");
        }
        return lat == null || lng == null ? null : new Near(lat, lng, radius == null ? 25 : radius);
    }

    private Availability availability() {
        List<String> days = days();
        LocalTime from = time("available_from");
        LocalTime to = time("available_to");
        if (from != null && to != null && !from.isBefore(to)) fail("available_to", "must be after available_from");
        return days.isEmpty() && from == null && to == null ? null : new Availability(days, from, to);
    }

    private List<String> days() {
        List<String> out = new ArrayList<>();
        String raw = text("available_days");
        for (String part : raw == null ? new String[0] : raw.split(",")) {
            String day = part.trim().toLowerCase(Locale.ROOT);
            if (day.isEmpty() || out.contains(day)) continue;
            if (WeekDay.tryFrom(day).isEmpty()) fail("available_days", "use monday to sunday, comma separated");
            out.add(day);
        }
        return out;
    }

    private LocalTime time(String name) {
        String value = text(name);
        LocalTime parsed = ClockTime.parse(value);
        if (value != null && parsed == null) fail(name, "must be HH:mm");
        return parsed;
    }

    private List<String> words(String text, boolean dropFiller) {
        if (text == null) return List.of();
        return Arrays.stream(NOT_WORD.split(text.toLowerCase(Locale.ROOT)))
                .filter(w -> w.length() >= 2 && !(dropFiller && FILLER.contains(w)))
                .map(w -> w.length() > 40 ? w.substring(0, 40) : w)
                .distinct().limit(MAX_WORDS).toList();
    }

    private String text(String name) {
        String value = params.getFirst(name);
        if (value == null || value.isBlank()) return null;
        if (value.length() > 200) fail(name, "too long");
        return value.trim();
    }

    private UUID uuid(String name) {
        String value = text(name);
        return value == null ? null : toUuid(name, value);
    }

    private Set<UUID> uuids(String name) {
        Set<UUID> out = new LinkedHashSet<>();
        for (String raw : params.getOrDefault(name, List.of())) {
            for (String part : raw.split(",")) {
                if (!part.isBlank()) Optional.ofNullable(toUuid(name, part.trim())).ifPresent(out::add);
            }
        }
        if (out.size() > MAX_VALUES) fail(name, "at most " + MAX_VALUES + " values");
        return out;
    }

    private UUID toUuid(String name, String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            fail(name, "must be a valid UUID");
            return null;
        }
    }

    private Integer integer(String name, int min, int max) {
        return QueryParser.integer(params, name, min, max, v);
    }

    private Double decimal(String name, double min, double max) {
        String value = text(name);
        if (value == null) return null;
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isFinite(parsed) && parsed >= min && parsed <= max) return parsed;
        } catch (NumberFormatException ignored) {
            // reported below
        }
        fail(name, "must be a number from " + min + " to " + max);
        return null;
    }

    private <T> Optional<T> fail(String name, String message) {
        v.check(false, FieldErrors.invalidFormat(name, message));
        return Optional.empty();
    }
}
