package com.kworkerharmony.backend.reference.organization;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrganizationDefinition(
        @JsonProperty("org_cd") String orgCd,
        @JsonProperty("name_ko") String nameKo,
        @JsonProperty("name_en") String nameEn,
        @JsonProperty("sms_yn") String smsYn,
        @JsonProperty("use_yn") String useYn
) {
}
