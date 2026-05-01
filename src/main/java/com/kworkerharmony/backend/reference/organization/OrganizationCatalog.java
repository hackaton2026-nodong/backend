package com.kworkerharmony.backend.reference.organization;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OrganizationCatalog {

    private final List<OrganizationDefinition> items;
    private final Map<String, OrganizationDefinition> byOrgCd;

    public OrganizationCatalog(ReferenceResourceReader resourceReader) {
        this.items = resourceReader.readList("reference/education/organization-codes.json", OrganizationDefinition.class);
        this.byOrgCd = items.stream()
                .collect(Collectors.toUnmodifiableMap(OrganizationDefinition::orgCd, Function.identity()));
    }

    public List<OrganizationDefinition> getAll() {
        return items;
    }

    public Optional<OrganizationDefinition> findByOrgCd(String code) {
        return Optional.ofNullable(byOrgCd.get(code));
    }
}
