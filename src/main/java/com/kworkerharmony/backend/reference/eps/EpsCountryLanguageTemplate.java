package com.kworkerharmony.backend.reference.eps;

import java.util.List;

public record EpsCountryLanguageTemplate(
        String countryCode,
        String countryNameKo,
        String countryNameEn,
        String primaryLanguageCode,
        List<String> supportedLanguageCodes,
        int displayOrder
) {
}
