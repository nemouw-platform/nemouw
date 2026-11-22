package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.specialist.discovery.DiscoveryDtos.SearchOptions;
import com.nemouw.specialist.discovery.DiscoveryDtos.SpecialistPublicProfile;
import com.nemouw.specialist.discovery.DiscoveryDtos.SpecialistSearchPage;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/specialists")
public class DiscoveryController {
    private final DiscoveryService search;
    private final PublicProfileService profiles;

    DiscoveryController(DiscoveryService search, PublicProfileService profiles) {
        this.search = search;
        this.profiles = profiles;
    }

    @GetMapping
    SpecialistSearchPage search(@RequestParam MultiValueMap<String, String> params) {
        return search.search(params);
    }

    @GetMapping("/search/fields")
    SearchOptions searchFields() {
        return search.options();
    }

    @GetMapping("/{username}")
    SpecialistPublicProfile profile(@PathVariable String username,
                                    @RequestParam(name = "locale", required = false) String locale) {
        return profiles.get(username, parseLocale(locale));
    }

    private static LocaleCode parseLocale(String locale) {
        if (locale == null || locale.isBlank()) return LocaleCode.EN;
        return LocaleCode.tryFrom(locale.trim())
                .orElseThrow(() -> Errors.invalidField("locale", "must be one of en, ar, fr"));
    }
}
