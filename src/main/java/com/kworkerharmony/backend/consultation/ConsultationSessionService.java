package com.kworkerharmony.backend.consultation;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.consultation.dto.request.CreateConsultationMessageRequest;
import com.kworkerharmony.backend.consultation.dto.request.CreateConsultationSessionRequest;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationMessageResponse;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationSessionDetailResponse;
import com.kworkerharmony.backend.consultation.dto.response.ConsultationSessionSummaryResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationSessionService {

    private final ConsultationSessionRepository consultationSessionRepository;
    private final ConsultationMessageRepository consultationMessageRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;

    @Transactional(readOnly = true)
    public List<ConsultationSessionSummaryResponse> getSessions(UserPrincipal userPrincipal) {
        return consultationSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userPrincipal.getId()).stream()
                .map(session -> ConsultationSessionSummaryResponse.from(
                        session,
                        consultationMessageRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId()).orElse(null)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationSessionDetailResponse getSession(String sessionId, UserPrincipal userPrincipal) {
        ConsultationSession session = getAccessibleSession(sessionId, userPrincipal);
        List<ConsultationMessageResponse> messages = consultationMessageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(ConsultationMessageResponse::from)
                .toList();
        return ConsultationSessionDetailResponse.from(session, messages);
    }

    @Transactional
    public ConsultationSessionDetailResponse createSession(
            CreateConsultationSessionRequest request,
            UserPrincipal userPrincipal
    ) {
        User user = getUser(userPrincipal);
        validateCaseAccess(request.caseId(), user);
        ConsultationSession session = consultationSessionRepository.save(new ConsultationSession(
                request.title(),
                request.caseId(),
                user
        ));
        if (request.initialMessage() != null && !request.initialMessage().isBlank()) {
            consultationMessageRepository.save(new ConsultationMessage(
                    session,
                    ConsultationMessageRole.USER,
                    request.initialMessage()
            ));
        }
        return getSession(session.getId(), userPrincipal);
    }

    @Transactional
    public ConsultationMessageResponse addMessage(
            String sessionId,
            CreateConsultationMessageRequest request,
            UserPrincipal userPrincipal
    ) {
        ConsultationSession session = getAccessibleSession(sessionId, userPrincipal);
        ConsultationMessage message = consultationMessageRepository.save(new ConsultationMessage(
                session,
                request.role(),
                request.content()
        ));
        return ConsultationMessageResponse.from(message);
    }

    private ConsultationSession getAccessibleSession(String sessionId, UserPrincipal userPrincipal) {
        ConsultationSession session = consultationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Consultation session not found"));
        if (!session.getUser().getId().equals(userPrincipal.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Consultation session belongs to another user");
        }
        return session;
    }

    private User getUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private void validateCaseAccess(String caseId, User user) {
        if (caseId == null || caseId.isBlank()) {
            return;
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        boolean sameCompany = user.getEnterprise() != null
                && caseEntity.getEnterprise().getId().equals(user.getEnterprise().getId());
        boolean isParty = (caseEntity.getEmployer() != null && caseEntity.getEmployer().getId().equals(user.getId()))
                || (caseEntity.getWorker() != null && caseEntity.getWorker().getId().equals(user.getId()));
        if (!sameCompany || !isParty) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Case access is limited to case parties");
        }
    }
}
