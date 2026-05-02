package com.kworkerharmony.backend.enterprise.dto.response;

import com.kworkerharmony.backend.user.User;
import java.time.LocalDate;

public record CompanyUserResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        String role,
        String userType,
        String status,
        String countryCode,
        String languageCode,
        LocalDate birthDate,
        LocalDate visaExpiresAt,
        Long enterpriseId,
        String enterpriseName
) {

    public static CompanyUserResponse from(User user) {
        return new CompanyUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getUserType().name(),
                user.getStatus().name(),
                user.getCountryCode(),
                user.getLanguageCode(),
                user.getBirthDate(),
                user.getVisaExpiresAt(),
                user.getEnterprise() == null ? null : user.getEnterprise().getId(),
                user.getEnterprise() == null ? null : user.getEnterprise().getName()
        );
    }
}
