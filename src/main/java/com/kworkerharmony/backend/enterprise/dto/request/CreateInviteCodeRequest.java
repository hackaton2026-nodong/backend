package com.kworkerharmony.backend.enterprise.dto.request;

import com.kworkerharmony.backend.user.Role;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record CreateInviteCodeRequest(
        String caseId,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDateTime expiresAt,

        @Positive(message = "Max uses must be positive")
        int maxUses,

        @NotNull(message = "Default role is required")
        Role defaultRole,

        String caseId
) {
}
