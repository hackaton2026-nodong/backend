package com.kworkerharmony.backend.consultation.dto.response;

import com.kworkerharmony.backend.consultation.ConsultationMessage;
import com.kworkerharmony.backend.consultation.ConsultationSession;
import java.time.LocalDateTime;

public record ConsultationSessionSummaryResponse(
        String id,
        String title,
        String caseId,
        String lastMessage,
        LocalDateTime updatedAt
) {

    public static ConsultationSessionSummaryResponse from(ConsultationSession session, ConsultationMessage lastMessage) {
        return new ConsultationSessionSummaryResponse(
                session.getId(),
                session.getTitle(),
                session.getCaseId(),
                lastMessage == null ? null : lastMessage.getContent(),
                session.getUpdatedAt()
        );
    }
}
