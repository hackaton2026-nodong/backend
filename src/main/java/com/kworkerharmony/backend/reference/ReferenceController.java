package com.kworkerharmony.backend.reference;

import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.reference.eps.EpsCountryLanguageCatalog;
import com.kworkerharmony.backend.reference.eps.EpsCountryLanguageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/references")
@RequiredArgsConstructor
public class ReferenceController {

    private final EpsCountryLanguageCatalog epsCountryLanguageCatalog;

    @GetMapping("/eps-country-languages")
    public ApiResponse<List<EpsCountryLanguageResponse>> getEpsCountryLanguages() {
        return ApiResponse.success(epsCountryLanguageCatalog.getAll());
    }

    @GetMapping("/eps-country-languages/{countryCode}")
    public ApiResponse<EpsCountryLanguageResponse> getEpsCountryLanguage(@PathVariable String countryCode) {
        return ApiResponse.success(epsCountryLanguageCatalog.getByCountryCode(countryCode));
    }
}
