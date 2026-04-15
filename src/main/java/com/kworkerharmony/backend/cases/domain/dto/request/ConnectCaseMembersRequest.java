package com.kworkerharmony.backend.cases.domain.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConnectCaseMembersRequest(
        @NotNull(message = "Employer id is required")
        Long employerId,

        @NotNull(message = "Worker id is required")
        Long workerId
) {
}
