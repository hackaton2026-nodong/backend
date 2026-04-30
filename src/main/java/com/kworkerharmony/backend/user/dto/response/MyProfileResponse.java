package com.kworkerharmony.backend.user.dto.response;

import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserStatus;
import com.kworkerharmony.backend.user.UserType;
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
        Long enterpriseId
) {

    public static MyProfileResponse from(User user) {
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
                user.getEnterprise() == null ? null : user.getEnterprise().getId()
        );
    }
}
