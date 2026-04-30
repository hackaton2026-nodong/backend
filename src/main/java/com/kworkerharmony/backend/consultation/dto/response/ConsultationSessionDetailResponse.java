package com.kworkerharmony.backend.consultation.dto.response;

import com.kworkerharmony.backend.consultation.ConsultationSession;
import java.time.LocalDateTime;
import java.util.List;

public record ConsultationSessionDetailResponse(
        String id,
        String title,
        String caseId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ConsultationMessageResponse> messages
) {

    public static ConsultationSessionDetailResponse from(
            ConsultationSession session,
            List<ConsultationMessageResponse> messages
    ) {
        return new ConsultationSessionDetailResponse(
                session.getId(),
                session.getTitle(),
                session.getCaseId(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                messages
        );
    }
}
