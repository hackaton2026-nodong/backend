package com.kworkerharmony.backend.dashboard.controller;

import com.kworkerharmony.backend.dashboard.domain.dto.response.DashboardSummaryResponse;
import com.kworkerharmony.backend.dashboard.domain.dto.response.WorkerDashboardResponse;
import com.kworkerharmony.backend.dashboard.service.DashboardService;
import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/worker")
    public ApiResponse<WorkerDashboardResponse> getWorkerDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(dashboardService.getWorkerDashboard(userPrincipal));
    }

    @GetMapping("/employer")
    public ApiResponse<DashboardSummaryResponse> getEmployerDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(dashboardService.getEmployerDashboard(userPrincipal));
    }
}
