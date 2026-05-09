package com.kworkerharmony.backend.cases.controller;

import com.kworkerharmony.backend.cases.domain.dto.request.CreateCaseRequest;
import com.kworkerharmony.backend.cases.domain.dto.request.ConnectCaseMembersRequest;
import com.kworkerharmony.backend.cases.domain.dto.response.CaseResponse;
import com.kworkerharmony.backend.cases.service.CaseService;
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
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping("/active")
    public ApiResponse<List<CaseResponse>> getActiveCases(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(caseService.getActiveCases(userPrincipal));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<CaseResponse> getCase(
            @PathVariable String caseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(caseService.getCase(caseId, userPrincipal));
    }

    @PostMapping
    public ApiResponse<CaseResponse> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(caseService.createCase(request, userPrincipal));
    }

    @PostMapping("/{caseId}/members")
    public ApiResponse<CaseResponse> connectMembers(
            @PathVariable String caseId,
            @Valid @RequestBody ConnectCaseMembersRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(caseService.connectMembers(caseId, request, userPrincipal));
    }
}
