package com.kworkerharmony.backend.reference.eps;

import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import com.kworkerharmony.backend.reference.language.LanguageCatalog;
import com.kworkerharmony.backend.reference.language.LanguageDefinition;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EpsCountryLanguageCatalog {

    private final LanguageCatalog languageCatalog;
    private final List<EpsCountryLanguageTemplate> templates;
    private final Map<String, EpsCountryLanguageTemplate> templatesByCountryCode;

    public EpsCountryLanguageCatalog(ReferenceResourceReader resourceReader, LanguageCatalog languageCatalog) {
        this.languageCatalog = languageCatalog;
        EpsCountryLanguageCatalogResource resource = resourceReader.read(
                "reference/eps-country-language-templates.json",
                EpsCountryLanguageCatalogResource.class
        );
        this.templates = resource.templates().stream()
                .sorted(Comparator.comparingInt(EpsCountryLanguageTemplate::displayOrder))
                .toList();
        this.templatesByCountryCode = this.templates.stream()
                .collect(Collectors.toUnmodifiableMap(template -> normalizeCountryCode(template.countryCode()), Function.identity()));
    }

    public List<EpsCountryLanguageResponse> getAll() {
        return templates.stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<EpsCountryLanguageResponse> findByCountryCode(String countryCode) {
        if (countryCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(templatesByCountryCode.get(normalizeCountryCode(countryCode)))
                .map(this::toResponse);
    }

    public EpsCountryLanguageResponse getByCountryCode(String countryCode) {
        return findByCountryCode(countryCode)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "EPS country language template not found"));
    }

    private EpsCountryLanguageResponse toResponse(EpsCountryLanguageTemplate template) {
        List<EpsCountryLanguageResponse.LanguageOption> languages = template.supportedLanguageCodes().stream()
                .map(languageCode -> toLanguageOption(languageCode, template.primaryLanguageCode()))
                .toList();
        return new EpsCountryLanguageResponse(
                normalizeCountryCode(template.countryCode()),
                template.countryNameKo(),
                template.countryNameEn(),
                languageCatalog.normalize(template.primaryLanguageCode()),
                languages
        );
    }

    private EpsCountryLanguageResponse.LanguageOption toLanguageOption(String languageCode, String primaryLanguageCode) {
        String normalizedCode = languageCatalog.normalize(languageCode);
        LanguageDefinition language = languageCatalog.findByCode(normalizedCode)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Language not found: " + normalizedCode));
        return new EpsCountryLanguageResponse.LanguageOption(
                normalizedCode,
                language.name(),
                language.nameKo(),
                language.nativeName(),
                normalizedCode.equals(languageCatalog.normalize(primaryLanguageCode))
        );
    }

    private String normalizeCountryCode(String countryCode) {
        return countryCode.trim().toUpperCase();
    }
}
