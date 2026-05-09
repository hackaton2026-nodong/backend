package com.kworkerharmony.backend.reference.country;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CountryCatalog {

    private final List<CountryDefinition> countries;
    private final Map<String, CountryDefinition> countriesByCode;

    public CountryCatalog(ReferenceResourceReader resourceReader) {
        CountryCatalogResource resource = resourceReader.read("reference/countries.json", CountryCatalogResource.class);
        this.countries = List.copyOf(resource.countries());
        this.countriesByCode = this.countries.stream()
                .collect(Collectors.toUnmodifiableMap(country -> normalize(country.code()), Function.identity()));
    }

    public List<CountryDefinition> getAll() {
        return countries;
    }

    public boolean exists(String code) {
        return code != null && countriesByCode.containsKey(normalize(code));
    }

    public Optional<CountryDefinition> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(countriesByCode.get(normalize(code)));
    }

    public String normalize(String code) {
        return code.trim().toUpperCase();
    }
}
