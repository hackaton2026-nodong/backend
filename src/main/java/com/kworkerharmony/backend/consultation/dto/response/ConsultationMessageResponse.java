package com.kworkerharmony.backend.consultation.dto.response;

import com.kworkerharmony.backend.consultation.ConsultationMessage;
import com.kworkerharmony.backend.consultation.ConsultationMessageRole;
import java.time.LocalDateTime;

public record ConsultationMessageResponse(
        String id,
        ConsultationMessageRole role,
        String content,
        LocalDateTime createdAt
) {

    public static ConsultationMessageResponse from(ConsultationMessage message) {
        return new ConsultationMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
