package com.kworkerharmony.backend.cases.service;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.domain.dto.request.CreateCaseRequest;
import com.kworkerharmony.backend.cases.domain.dto.request.ConnectCaseMembersRequest;
import com.kworkerharmony.backend.cases.domain.dto.response.CaseResponse;
import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.Role;
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
    public CaseResponse getCase(String caseId, UserPrincipal userPrincipal) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        validateCompanyAccess(foundCase, getUser(userPrincipal));
        return CaseResponse.from(foundCase);
    }

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, UserPrincipal userPrincipal) {
        User creator = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        Enterprise enterprise = requireEnterprise(creator);

        User employer = null;
        User worker = null;
        if (creator.getRole() == Role.ADMIN || creator.getUserType() == UserType.EMPLOYER) {
            employer = creator;
        } else if (creator.getUserType() == UserType.WORKER) {
            worker = creator;
        }

        Case savedCase = caseRepository.save(new Case(
                enterprise,
                employer,
                worker,
                CaseStatus.PENDING,
                request.industry(),
                request.region()
        ));

        return CaseResponse.from(savedCase);
    }

    @Transactional
    public CaseResponse connectMembers(String caseId, ConnectCaseMembersRequest request, UserPrincipal userPrincipal) {
        Case foundCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        User actor = getUser(userPrincipal);
        validateCompanyAccess(foundCase, actor);
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
        if (employer.getEnterprise() == null
                || worker.getEnterprise() == null
                || !employer.getEnterprise().getId().equals(foundCase.getEnterprise().getId())
                || !worker.getEnterprise().getId().equals(foundCase.getEnterprise().getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Case members must belong to the same company");
        }

        foundCase.connectMembers(employer, worker);
        return CaseResponse.from(foundCase);
    }

    private User getUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private Enterprise requireEnterprise(User user) {
        if (user.getEnterprise() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "User is not assigned to a company");
        }
        return user.getEnterprise();
    }

    private void validateCompanyAccess(Case caseEntity, User user) {
        Enterprise enterprise = requireEnterprise(user);
        if (!caseEntity.getEnterprise().getId().equals(enterprise.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Case belongs to another company");
        }
    }
}
