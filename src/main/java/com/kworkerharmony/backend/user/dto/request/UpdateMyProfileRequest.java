package com.kworkerharmony.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateMyProfileRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        String countryCode,

        @NotBlank
        String languageCode,

        @NotBlank
        @Size(max = 30)
        String phoneNumber,

        LocalDate visaExpiresAt
) {
}
