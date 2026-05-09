package com.kworkerharmony.backend.enterprise.dto.response;

import com.kworkerharmony.backend.enterprise.Enterprise;

public record EnterpriseResponse(
        Long id,
        String name,
        String businessNumber,
        String industry,
        String address,
        Integer foreignWorkerQuota,
        String employmentPermitCertNo,
        String countryCode,
        String languageCode,
        String status
) {

    public static EnterpriseResponse from(Enterprise enterprise) {
        return new EnterpriseResponse(
                enterprise.getId(),
                enterprise.getName(),
                enterprise.getBusinessNumber(),
                enterprise.getIndustry(),
                enterprise.getAddress(),
                enterprise.getForeignWorkerQuota(),
                enterprise.getEmploymentPermitCertNo(),
                enterprise.getCountryCode(),
                enterprise.getLanguageCode(),
                enterprise.getStatus().name()
        );
    }
}
