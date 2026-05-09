package com.kworkerharmony.backend.consultation;

import com.kworkerharmony.backend.consultation.dto.request.CreateConsultationRequest;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping
    public ApiResponse<List<ConsultationResponse>> getConsultations() {
        return ApiResponse.success(consultationService.getConsultations());
    }

    @GetMapping("/{consultationId}")
    public ApiResponse<ConsultationResponse> getConsultation(@PathVariable Long consultationId) {
        return ApiResponse.success(consultationService.getConsultation(consultationId));
    }

    @PostMapping
    public ApiResponse<ConsultationResponse> createConsultation(@Valid @RequestBody CreateConsultationRequest request) {
        return ApiResponse.success(consultationService.createConsultation(request));
    }
}
