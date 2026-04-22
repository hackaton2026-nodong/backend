package com.kworkerharmony.backend.consultation.dto.response;

import com.kworkerharmony.backend.consultation.Consultation;

public record ConsultationResponse(
        Long id,
        String diagnose,
        Long userId
) {

    public static ConsultationResponse from(Consultation consultation) {
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getDiagnose(),
                consultation.getUser().getId()
        );
    }
}
