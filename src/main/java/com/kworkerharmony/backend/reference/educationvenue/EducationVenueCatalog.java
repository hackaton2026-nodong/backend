package com.kworkerharmony.backend.reference.educationvenue;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class EducationVenueCatalog {

    private final List<EducationVenueDefinition> items;
    private final Map<String, EducationVenueDefinition> byEduOrgCd;

    public EducationVenueCatalog(ReferenceResourceReader resourceReader) {
        this.items = resourceReader.readList("reference/education/education-venues.json", EducationVenueDefinition.class);
        this.byEduOrgCd = items.stream()
                .collect(Collectors.toUnmodifiableMap(EducationVenueDefinition::eduOrgCd, Function.identity()));
    }

    public List<EducationVenueDefinition> getAll() {
        return items;
    }

    public Optional<EducationVenueDefinition> findByEduOrgCd(String code) {
        return Optional.ofNullable(byEduOrgCd.get(code));
    }

    public List<EducationVenueDefinition> findByOrgCd(String orgCd) {
        return items.stream()
                .filter(v -> v.orgCd().equals(orgCd))
                .toList();
    }

    public List<EducationVenueDefinition> findByRegion(String region, int limit) {
        Stream<EducationVenueDefinition> stream = items.stream();
        if (region != null && !region.isBlank()) {
            stream = stream.filter(v -> v.address().contains(region) || v.name().contains(region));
        }
        return stream.limit(limit).toList();
    }
}
