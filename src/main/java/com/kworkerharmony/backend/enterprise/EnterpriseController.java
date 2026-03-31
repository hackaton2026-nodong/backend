package com.kworkerharmony.backend.enterprise;

import com.kworkerharmony.backend.enterprise.dto.request.CreateEnterpriseRequest;
import com.kworkerharmony.backend.enterprise.dto.response.EnterpriseResponse;
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
@RequestMapping("/enterprises")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @GetMapping
    public ApiResponse<List<EnterpriseResponse>> getEnterprises() {
        return ApiResponse.success(enterpriseService.getEnterprises());
    }

    @GetMapping("/{enterpriseId}")
    public ApiResponse<EnterpriseResponse> getEnterprise(@PathVariable Long enterpriseId) {
        return ApiResponse.success(enterpriseService.getEnterprise(enterpriseId));
    }

    @PostMapping
    public ApiResponse<EnterpriseResponse> createEnterprise(@Valid @RequestBody CreateEnterpriseRequest request) {
        return ApiResponse.success(enterpriseService.createEnterprise(request));
    }
}
