package com.kworkerharmony.backend.reference.country;

import java.util.List;

public record CountryCatalogResource(
        List<CountryDefinition> countries
) {
}
