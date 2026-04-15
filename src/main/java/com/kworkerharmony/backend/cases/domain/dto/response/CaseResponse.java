package com.kworkerharmony.backend.cases.domain.dto.response;

import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.entity.Case;
import java.time.LocalDateTime;

public record CaseResponse(
        String id,
        Long employerId,
        Long workerId,
        CaseStatus status,
        String industry,
        String region,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CaseResponse from(Case foundCase) {
        return new CaseResponse(
                foundCase.getId(),
                foundCase.getEmployer() != null ? foundCase.getEmployer().getId() : null,
                foundCase.getWorker() != null ? foundCase.getWorker().getId() : null,
                foundCase.getStatus(),
                foundCase.getIndustry(),
                foundCase.getRegion(),
                foundCase.getCreatedAt(),
                foundCase.getUpdatedAt()
        );
    }
}
