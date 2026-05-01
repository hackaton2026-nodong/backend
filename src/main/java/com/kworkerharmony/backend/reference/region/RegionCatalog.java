package com.kworkerharmony.backend.reference.region;

import com.kworkerharmony.backend.reference.ReferenceResourceReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RegionCatalog {

    private final List<RegionDefinition> items;
    private final Map<String, RegionDefinition> byRegionCd;

    public RegionCatalog(ReferenceResourceReader resourceReader) {
        this.items = resourceReader.readList("reference/education/region-codes.json", RegionDefinition.class);
        this.byRegionCd = items.stream()
                .collect(Collectors.toUnmodifiableMap(RegionDefinition::regionCd, Function.identity()));
    }

    public List<RegionDefinition> getAll() {
        return items;
    }

    public Optional<RegionDefinition> findByRegionCd(String code) {
        return Optional.ofNullable(byRegionCd.get(code));
    }
}
