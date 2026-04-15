package com.kworkerharmony.backend.alert.controller;

import com.kworkerharmony.backend.alert.domain.dto.response.AlertResponse;
import com.kworkerharmony.backend.alert.service.AlertService;
import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ApiResponse<List<AlertResponse>> getNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(alertService.getNotifications(userPrincipal));
    }

    @PatchMapping("/{alertId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable String alertId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        alertService.markAsRead(alertId, userPrincipal);
        return ApiResponse.empty();
    }
}
