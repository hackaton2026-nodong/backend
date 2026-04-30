package com.kworkerharmony.backend.reference.checklist;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ChecklistCatalog {

    private final ChecklistCatalogResource catalog;
    private final List<ChecklistItemDefinition> items;
    private final Map<String, ChecklistItemDefinition> itemsByCode;

    public ChecklistCatalog(ReferenceResourceReader resourceReader) {
        this.catalog = resourceReader.read(
                "reference/checklists/moel-foreign-worker-employment-management.json",
                ChecklistCatalogResource.class
        );
        this.items = catalog.sections().stream()
                .sorted(Comparator.comparingInt(ChecklistSectionDefinition::displayOrder))
                .flatMap(section -> section.items().stream()
                        .sorted(Comparator.comparingInt(ChecklistItemDefinition::displayOrder))
                        .map(item -> new ChecklistItemDefinition(
                                section.code(),
                                section.title(),
                                normalize(item.code()),
                                item.title(),
                                item.description(),
                                item.required(),
                                item.displayOrder(),
                                item.triggerTypeOrDefault()
                        )))
                .toList();
        this.itemsByCode = this.items.stream()
                .collect(Collectors.toUnmodifiableMap(ChecklistItemDefinition::code, Function.identity()));
    }

    public String catalogCode() {
        return catalog.catalogCode();
    }

    public String version() {
        return catalog.version();
    }

    public String title() {
        return catalog.title();
    }

    public List<ChecklistItemDefinition> getItems() {
        return items;
    }

    public Optional<ChecklistItemDefinition> findItem(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemsByCode.get(normalize(code)));
    }

    public boolean exists(String code) {
        return code != null && itemsByCode.containsKey(normalize(code));
    }

    public String normalize(String code) {
        return code.trim().toUpperCase();
    }
}
