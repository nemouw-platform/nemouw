package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.Codes;
import com.nemouw.common.enums.Currency;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.time.PlatformTime;
import com.nemouw.media.api.PublicMediaUrls;
import com.nemouw.specialist.discovery.DiscoveryDtos.SearchOptions;
import com.nemouw.specialist.discovery.DiscoveryDtos.SpecialistSearchPage;
import com.nemouw.specialist.discovery.DiscoveryDtos.SpecialistSearchResult;
import com.nemouw.specialist.discovery.DiscoveryDtos.SearchResultLocation;
import com.nemouw.specialist.discovery.SpecialistSearchRepository.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class DiscoveryService {
    private final SpecialistSearchRepository repository;
    private final PublicMediaUrls mediaUrls;
    private final PlatformTime clock;

    DiscoveryService(SpecialistSearchRepository repository, PublicMediaUrls mediaUrls, PlatformTime clock) {
        this.repository = repository;
        this.mediaUrls = mediaUrls;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    SpecialistSearchPage search(MultiValueMap<String, String> rawParams) {
        SearchParams params = SearchParamsParser.parse(rawParams);
        SearchSql.Query query = SearchSql.build(params, clock.now());

        List<Row> rows = repository.page(query);
        long total = repository.count(query);
        List<UUID> ids = rows.stream().map(Row::id).toList();
        Map<UUID, List<UUID>> conditions = repository.conditionIds(ids);
        Map<UUID, List<SearchResultLocation>> locations = repository.locations(ids);

        List<SpecialistSearchResult> data = rows.stream()
                .map(r -> toResult(r, conditions.getOrDefault(r.id(), List.of()), locations.getOrDefault(r.id(), List.of())))
                .toList();
        return new SpecialistSearchPage(data, params.query().page(), params.query().pageSize(), total);
    }

    SearchOptions options() {
        return new SearchOptions(SearchFields.FIELDS.describe(), SearchFields.FIELDS.computedSorts());
    }

    private SpecialistSearchResult toResult(Row r, List<UUID> conditionIds, List<SearchResultLocation> locations) {
        Double km = r.distanceKm() == null ? null
                : BigDecimal.valueOf(r.distanceKm()).setScale(1, RoundingMode.HALF_UP).doubleValue();
        return new SpecialistSearchResult(r.id(), r.username(), r.firstName(), r.lastName(), r.title(),
                r.headline(), r.specialtyId(), mediaUrls.urlFor(r.profilePictureKey()), r.yearsOfExperience(),
                r.city(), r.countryId(), km, Codes.orNull(LocaleCode.class, r.primaryLanguage()), r.sessionPriceMin(),
                r.sessionPriceMax(), Codes.fromCode(Currency.class, r.priceCurrency()), r.minAgeTreated(),
                r.maxAgeTreated(), conditionIds, locations);
    }
}
