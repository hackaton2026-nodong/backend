package com.kworkerharmony.backend.user.dto.response;

import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserStatus;
import com.kworkerharmony.backend.user.UserType;
import com.kworkerharmony.backend.enterprise.Enterprise;
import java.time.LocalDate;

public record MyProfileResponse(
        Long id,
        String email,
        String name,
        Role role,
        UserType userType,
        UserStatus status,
        String countryCode,
        String languageCode,
        String phoneNumber,
        LocalDate visaExpiresAt,
        Long enterpriseId,
        String enterpriseName,
        String enterpriseIndustry,
        String enterpriseAddress,
        String businessNumber
) {

    public static MyProfileResponse from(User user) {
        Enterprise enterprise = user.getEnterprise();
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getUserType(),
                user.getStatus(),
                user.getCountryCode(),
                user.getLanguageCode(),
                user.getPhoneNumber(),
                user.getVisaExpiresAt(),
                enterprise == null ? null : enterprise.getId(),
                enterprise == null ? null : enterprise.getName(),
                enterprise == null ? null : enterprise.getIndustry(),
                enterprise == null ? null : enterprise.getAddress(),
                enterprise == null ? null : enterprise.getBusinessNumber()
        );
    }
}
