package com.kworkerharmony.backend.reference.region;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegionDefinition(
        @JsonProperty("region_cd") String regionCd,
        @JsonProperty("name_ko") String nameKo,
        @JsonProperty("name_en") String nameEn,
        @JsonProperty("use_yn") String useYn,
        @JsonProperty("name_zh") String nameZh,
        @JsonProperty("name_ru") String nameRu
) {
}
