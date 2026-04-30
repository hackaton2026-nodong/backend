package com.kworkerharmony.backend.enterprise;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.enterprise.dto.request.CreateEnterpriseRequest;
import com.kworkerharmony.backend.enterprise.dto.request.CreateInviteCodeRequest;
import com.kworkerharmony.backend.enterprise.dto.request.JoinCompanyRequest;
import com.kworkerharmony.backend.enterprise.dto.response.CompanyInviteCodeResponse;
import com.kworkerharmony.backend.enterprise.dto.response.CompanyUserResponse;
import com.kworkerharmony.backend.enterprise.dto.response.EnterpriseResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.reference.country.CountryCatalog;
import com.kworkerharmony.backend.reference.language.LanguageCatalog;
import com.kworkerharmony.backend.user.Role;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import com.kworkerharmony.backend.user.UserType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;
    private final CompanyInviteCodeRepository companyInviteCodeRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CountryCatalog countryCatalog;
    private final LanguageCatalog languageCatalog;

    @Transactional(readOnly = true)
    public List<EnterpriseResponse> getEnterprises() {
        return enterpriseRepository.findAll().stream()
                .map(EnterpriseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnterpriseResponse getEnterprise(Long enterpriseId) {
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Enterprise not found"));
        return EnterpriseResponse.from(enterprise);
    }

    @Transactional
    public EnterpriseResponse createEnterprise(CreateEnterpriseRequest request) {
        Enterprise enterprise = enterpriseRepository.save(
                new Enterprise(
                        request.name(),
                        request.businessNumber(),
                        request.industry(),
                        request.address(),
                        request.foreignWorkerQuota(),
                        request.employmentPermitCertNo(),
                        validatedCountryCode(request.countryCode()),
                        validatedLanguageCode(request.languageCode()),
                        EnterpriseStatus.ACTIVE
                )
        );
        return EnterpriseResponse.from(enterprise);
    }

    @Transactional
    public CompanyInviteCodeResponse createInviteCode(CreateInviteCodeRequest request, UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateAdmin(user);
        Enterprise enterprise = requireEnterprise(user);
        String caseId = validatedInviteCaseId(request.caseId(), enterprise);

        CompanyInviteCode inviteCode = companyInviteCodeRepository.save(new CompanyInviteCode(
                enterprise,
                caseId,
                UUID.randomUUID().toString().replace("-", ""),
                request.expiresAt(),
                request.maxUses(),
                0,
                true,
                request.defaultRole()
        ));
        return CompanyInviteCodeResponse.from(inviteCode);
    }

    @Transactional
    public EnterpriseResponse joinCompany(JoinCompanyRequest request, UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        if (user.getEnterprise() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "User already belongs to a company");
        }

        CompanyInviteCode inviteCode = companyInviteCodeRepository.findByCode(request.inviteCode())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Invite code not found"));

        if (!inviteCode.isUsableAt(java.time.LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Invite code is expired or exhausted");
        }

        user.assignEnterprise(inviteCode.getEnterprise());
        user.changeRole(inviteCode.getDefaultRole());
        user.changeUserType(toUserType(inviteCode.getDefaultRole()));
        user.activate();
        inviteCode.use();
        connectCaseIfRequired(inviteCode, user);

        return EnterpriseResponse.from(inviteCode.getEnterprise());
    }

    @Transactional(readOnly = true)
    public List<CompanyUserResponse> getCompanyUsers(UserPrincipal userPrincipal) {
        User user = getUser(userPrincipal);
        validateAdmin(user);
        Enterprise enterprise = requireEnterprise(user);

        return userRepository.findAllByEnterpriseId(enterprise.getId()).stream()
                .map(CompanyUserResponse::from)
                .toList();
    }

    private User getUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    private void validateAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Company admin access is required");
        }
    }

    private Enterprise requireEnterprise(User user) {
        if (user.getEnterprise() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "User is not assigned to a company");
        }
        return user.getEnterprise();
    }

    private String validatedInviteCaseId(String caseId, Enterprise enterprise) {
        if (caseId == null || caseId.isBlank()) {
            return null;
        }

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        if (!caseEntity.getEnterprise().getId().equals(enterprise.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Case belongs to another company");
        }
        return caseEntity.getId();
    }

    private void connectCaseIfRequired(CompanyInviteCode inviteCode, User user) {
        if (inviteCode.getCaseId() == null || inviteCode.getCaseId().isBlank()) {
            return;
        }

        Case caseEntity = caseRepository.findById(inviteCode.getCaseId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        if (!caseEntity.getEnterprise().getId().equals(inviteCode.getEnterprise().getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "Invite code case belongs to another company");
        }
        caseEntity.connectWorker(user);
    }

    private UserType toUserType(Role role) {
        return switch (role) {
            case ADMIN, EMPLOYER -> UserType.EMPLOYER;
            case WORKER -> UserType.WORKER;
        };
    }

    private String validatedCountryCode(String countryCode) {
        if (!countryCatalog.exists(countryCode)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Country not found");
        }
        return countryCatalog.normalize(countryCode);
    }

    private String validatedLanguageCode(String languageCode) {
        if (!languageCatalog.exists(languageCode)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Language not found");
        }
        return languageCatalog.normalize(languageCode);
    }
}
