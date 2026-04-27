package com.kworkerharmony.backend.reference.eps;

import java.util.List;

public record EpsCountryLanguageResponse(
        String countryCode,
        String countryNameKo,
        String countryNameEn,
        String primaryLanguageCode,
        List<LanguageOption> languages
) {

    public record LanguageOption(
            String code,
            String name,
            String nameKo,
            String nativeName,
            boolean primary
    ) {
    }
}
