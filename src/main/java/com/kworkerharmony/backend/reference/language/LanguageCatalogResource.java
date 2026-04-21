package com.kworkerharmony.backend.reference.language;

import java.util.List;

public record LanguageCatalogResource(
        List<LanguageDefinition> languages
) {
}
