package com.kworkerharmony.backend.reference.educationvenue;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EducationVenueDefinition(
        @JsonProperty("edu_org_cd") String eduOrgCd,
        @JsonProperty("org_cd") String orgCd,
        String name,
        @JsonProperty("zip_cd") String zipCd,
        String address
) {
}
