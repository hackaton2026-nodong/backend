package com.kworkerharmony.backend.auth.dto.request;

import com.kworkerharmony.backend.user.UserType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 100)
        String name,

        UserType userType,

        @NotBlank
        String countryCode,

        @NotBlank
        String languageCode,

        String inviteCode,

        String companyName,

        String companyBusinessNumber,

        String companyIndustry,

        String companyCountryCode,

        String companyLanguageCode
) {

    @AssertTrue(message = "Provide either inviteCode or all company fields")
    public boolean isSignupFlowValid() {
        boolean hasInviteCode = inviteCode != null && !inviteCode.isBlank();
        boolean hasCompanyFields = hasText(companyName)
                && hasText(companyBusinessNumber)
                && hasText(companyIndustry)
                && hasText(companyCountryCode)
                && hasText(companyLanguageCode);

        return hasInviteCode ^ hasCompanyFields;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
