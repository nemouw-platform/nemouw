package com.nemouw.country;

import com.nemouw.country.CountryDtos.CountryResponse;
import com.nemouw.country.CountryDtos.CountryWriteRequest;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.util.Validator;
import com.nemouw.country.CountryLookup.CountryExistsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class CountryService {
    private static final Sort BY_NAME = Sort.by("name");

    private final CountryRepository countries;

    CountryService(CountryRepository countries) {
        this.countries = countries;
    }

    @Transactional(readOnly = true)
    List<CountryResponse> list(Boolean active) {
        List<Country> rows = active == null ? countries.findAll(BY_NAME) : countries.findByActive(active, BY_NAME);
        return rows.stream().map(CountryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    CountryResponse get(UUID id) {
        return CountryResponse.from(find(id));
    }

    @Transactional
    CountryResponse create(CountryWriteRequest req) {
        validate(req, true);
        String iso = req.isoCode().trim().toUpperCase(Locale.ROOT);
        if (countries.existsByIsoCode(iso)) throw new ApiException(ErrorCode.ISO_CODE_TAKEN);

        Country country = Country.create();
        apply(country, req);
        try {
            return CountryResponse.from(countries.saveAndFlush(country));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.ISO_CODE_TAKEN);   // lost a race with another create
        }
    }

    @Transactional
    CountryResponse update(UUID id, CountryWriteRequest req) {
        validate(req, false);
        Country country = find(id);
        if (req.isoCode() != null) {
            String iso = req.isoCode().trim().toUpperCase(Locale.ROOT);
            if (countries.existsByIsoCodeAndIdNot(iso, id)) throw new ApiException(ErrorCode.ISO_CODE_TAKEN);
        }
        apply(country, req);
        try {
            countries.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.ISO_CODE_TAKEN);
        }
        return CountryResponse.from(country);
    }

    @Transactional
    void delete(UUID id) {
        Country country = find(id);
        try {
            countries.delete(country);
            countries.flush();   // force the foreign key check now, not at commit
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.COUNTRY_IN_USE);
        }
    }

    private Country find(UUID id) {
        return countries.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    /**
     * Copies only the fields that were sent. Validation has already run.
     */
    private static void apply(Country c, CountryWriteRequest r) {
        if (r.name() != null) c.setName(r.name().trim());
        if (r.isoCode() != null) c.setIsoCode(r.isoCode().trim().toUpperCase(Locale.ROOT));
        if (r.currencyCode() != null) c.setCurrencyCode(r.currencyCode().trim().toUpperCase(Locale.ROOT));
        if (r.phoneCode() != null) c.setPhoneCode(r.phoneCode().trim());
        if (r.region() != null) c.setRegion(r.region().isBlank() ? null : r.region().trim());
        if (r.active() != null) c.setActive(r.active());
    }

    private static void validate(CountryWriteRequest r, boolean create) {
        new Validator()
                .requiredForWrite(create, "name", r.name())
                .requiredForWrite(create, "iso_code", r.isoCode())
                .requiredForWrite(create, "currency_code", r.currencyCode())
                .requiredForWrite(create, "phone_code", r.phoneCode())
                .maxLength("name", r.name(), TEXT_MAX)
                .maxLength("region", r.region(), TEXT_MAX)
                .isoCode("iso_code", r.isoCode())
                .currencyCode("currency_code", r.currencyCode())
                .phoneCode("phone_code", r.phoneCode())
                .throwIfInvalid();
    }

    @Bean
    CountryExistsProvider countryExistsProvider() {
        return countries::existsById;
    }
}