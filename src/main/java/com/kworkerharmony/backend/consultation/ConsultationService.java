package com.kworkerharmony.backend.consultation;

import com.kworkerharmony.backend.consultation.dto.request.CreateConsultationRequest;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ConsultationResponse> getConsultations() {
        return consultationRepository.findAll().stream()
                .map(ConsultationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultation(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Consultation not found"));
        return ConsultationResponse.from(consultation);
    }

    @Transactional
    public ConsultationResponse createConsultation(CreateConsultationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        Consultation consultation = consultationRepository.save(new Consultation(request.diagnose(), user));
        return ConsultationResponse.from(consultation);
    }
}
