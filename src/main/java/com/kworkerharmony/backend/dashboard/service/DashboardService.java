package com.kworkerharmony.backend.dashboard.service;

import com.kworkerharmony.backend.alert.domain.AlertRepository;
import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.checklist.domain.CaseChecklistItemRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.dashboard.domain.dto.response.DashboardSummaryResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CaseRepository caseRepository;
    private final CaseChecklistItemRepository caseChecklistItemRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getWorkerDashboard(UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateUserType(user, UserType.WORKER, "Worker dashboard is only available for worker accounts");

        return new DashboardSummaryResponse(
                user.getId(),
                user.getUserType(),
                caseRepository.countByWorkerIdAndStatus(user.getId(), CaseStatus.ACTIVE),
                caseChecklistItemRepository.countByCaseEntityWorkerId(user.getId()),
                caseChecklistItemRepository.countByCaseEntityWorkerIdAndStatus(user.getId(), ChecklistStatus.COMPLETED),
                alertRepository.countByUserIdAndIsReadFalse(user.getId())
        );
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getEmployerDashboard(UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateUserType(user, UserType.EMPLOYER, "Employer dashboard is only available for employer accounts");

        return new DashboardSummaryResponse(
                user.getId(),
                user.getUserType(),
                caseRepository.countByEmployerIdAndStatus(user.getId(), CaseStatus.ACTIVE),
                caseChecklistItemRepository.countByCaseEntityEmployerId(user.getId()),
                caseChecklistItemRepository.countByCaseEntityEmployerIdAndStatus(user.getId(), ChecklistStatus.COMPLETED),
                alertRepository.countByUserIdAndIsReadFalse(user.getId())
        );
    }

    private User getUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private void validateUserType(User user, UserType expectedType, String message) {
        if (user.getUserType() != expectedType) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, message);
        }
    }
}
