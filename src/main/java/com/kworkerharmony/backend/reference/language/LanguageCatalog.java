package com.kworkerharmony.backend.reference.language;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LanguageCatalog {

    private final List<LanguageDefinition> languages;
    private final Map<String, LanguageDefinition> languagesByCode;

    public LanguageCatalog(ReferenceResourceReader resourceReader) {
        LanguageCatalogResource resource = resourceReader.read("reference/languages.json", LanguageCatalogResource.class);
        this.languages = List.copyOf(resource.languages());
        this.languagesByCode = this.languages.stream()
                .collect(Collectors.toUnmodifiableMap(language -> normalize(language.code()), Function.identity()));
    }

    public List<LanguageDefinition> getAll() {
        return languages;
    }

    public boolean exists(String code) {
        return code != null && languagesByCode.containsKey(normalize(code));
    }

    public Optional<LanguageDefinition> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(languagesByCode.get(normalize(code)));
    }

    public String normalize(String code) {
        return code.trim().toLowerCase();
    }
}
