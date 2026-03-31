package com.kworkerharmony.backend.enterprise.dto.response;

import com.kworkerharmony.backend.enterprise.Enterprise;

public record EnterpriseResponse(
        Long id,
        String name,
        String contact,
        String location
) {

    public static EnterpriseResponse from(Enterprise enterprise) {
        return new EnterpriseResponse(
                enterprise.getId(),
                enterprise.getName(),
                enterprise.getContact(),
                enterprise.getLocation()
        );
    }
}
