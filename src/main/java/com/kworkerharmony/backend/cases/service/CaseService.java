package com.kworkerharmony.backend.cases.service;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.dto.request.CreateCaseRequest;
import com.kworkerharmony.backend.cases.domain.dto.request.ConnectCaseMembersRequest;
import com.kworkerharmony.backend.cases.domain.dto.response.CaseResponse;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CaseResponse> getActiveCases(UserPrincipal userPrincipal) {
        return caseRepository.findActiveCasesByUserId(userPrincipal.getId(), CaseStatus.ACTIVE).stream()
                .map(CaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(String caseId) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        return CaseResponse.from(foundCase);
    }

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, UserPrincipal userPrincipal) {
        User creator = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        User employer = null;
        User worker = null;
        if (creator.getUserType() == UserType.EMPLOYER) {
            employer = creator;
        } else if (creator.getUserType() == UserType.WORKER) {
            worker = creator;
        }

        Case savedCase = caseRepository.save(new Case(
                employer,
                worker,
                CaseStatus.PENDING,
                request.industry(),
                request.region()
        ));

        return CaseResponse.from(savedCase);
    }

    @Transactional
    public CaseResponse connectMembers(String caseId, ConnectCaseMembersRequest request) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        User employer = userRepository.findById(request.employerId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Employer not found"));
        User worker = userRepository.findById(request.workerId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Worker not found"));

        if (employer.getUserType() != UserType.EMPLOYER) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Selected employer user is not an employer");
        }
        if (worker.getUserType() != UserType.WORKER) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Selected worker user is not a worker");
        }

        foundCase.connectMembers(employer, worker);
        return CaseResponse.from(foundCase);
    }
}
