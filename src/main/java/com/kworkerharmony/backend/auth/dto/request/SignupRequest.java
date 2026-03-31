package com.kworkerharmony.backend.auth.dto.request;

import com.kworkerharmony.backend.user.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

        @NotNull
        UserType userType,

        @NotBlank
        String countryCode
) {
}
