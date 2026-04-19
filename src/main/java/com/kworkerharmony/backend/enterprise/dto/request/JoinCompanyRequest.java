package com.kworkerharmony.backend.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinCompanyRequest(
        @NotBlank(message = "Invite code is required")
        String inviteCode
) {
}
