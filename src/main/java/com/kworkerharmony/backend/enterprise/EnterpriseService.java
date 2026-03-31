package com.kworkerharmony.backend.enterprise;

import com.kworkerharmony.backend.enterprise.dto.request.CreateEnterpriseRequest;
import com.kworkerharmony.backend.enterprise.dto.response.EnterpriseResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;

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
                new Enterprise(request.name(), request.contact(), request.location())
        );
        return EnterpriseResponse.from(enterprise);
    }
}
