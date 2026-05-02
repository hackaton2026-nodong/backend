package com.kworkerharmony.backend.user.dto.response;

import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.user.User;
import java.time.LocalDate;

public record CurrentUserResponse(
        Long id,
        String email,
        String name,
        LocalDate birthDate,
        String phoneNumber,
        LocalDate visaExpiresAt,
        String role,
        String userType,
        String status,
        String countryCode,
        String languageCode,
        Long enterpriseId,
        String enterpriseName,
        String enterpriseIndustry,
        String enterpriseAddress,
        String businessNumber
) {

    public static CurrentUserResponse from(User user) {
        Enterprise enterprise = user.getEnterprise();
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getBirthDate(),
                user.getPhoneNumber(),
                user.getVisaExpiresAt(),
                user.getRole().name(),
                user.getUserType().name(),
                user.getStatus().name(),
                user.getCountryCode(),
                user.getLanguageCode(),
                enterprise == null ? null : enterprise.getId(),
                enterprise == null ? null : enterprise.getName(),
                enterprise == null ? null : enterprise.getIndustry(),
                enterprise == null ? null : enterprise.getAddress(),
                enterprise == null ? null : enterprise.getBusinessNumber()
        );
    }
}
