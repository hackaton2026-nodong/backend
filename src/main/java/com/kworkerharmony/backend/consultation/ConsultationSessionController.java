package com.kworkerharmony.backend.consultation;

import com.kworkerharmony.backend.consultation.dto.request.CreateConsultationMessageRequest;
import com.kworkerharmony.backend.consultation.dto.request.CreateConsultationSessionRequest;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationMessageResponse;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationSessionDetailResponse;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationSessionSummaryResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultation-sessions")
@RequiredArgsConstructor
public class ConsultationSessionController {

    private final ConsultationSessionService consultationSessionService;

    @GetMapping
    public ApiResponse<List<ConsultationSessionSummaryResponse>> getSessions(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(consultationSessionService.getSessions(userPrincipal));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<ConsultationSessionDetailResponse> getSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(consultationSessionService.getSession(sessionId, userPrincipal));
    }

    @PostMapping
    public ApiResponse<ConsultationSessionDetailResponse> createSession(
            @Valid @RequestBody CreateConsultationSessionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(consultationSessionService.createSession(request, userPrincipal));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<ConsultationMessageResponse> addMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody CreateConsultationMessageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(consultationSessionService.addMessage(sessionId, request, userPrincipal));
    }
}
