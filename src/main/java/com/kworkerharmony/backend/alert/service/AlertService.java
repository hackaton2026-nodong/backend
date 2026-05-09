package com.kworkerharmony.backend.alert.service;

import com.kworkerharmony.backend.alert.domain.AlertRepository;
import com.kworkerharmony.backend.alert.domain.AlertType;
import com.kworkerharmony.backend.alert.domain.dto.request.CreateAlertRequest;
import com.kworkerharmony.backend.alert.domain.dto.response.AlertResponse;
import com.kworkerharmony.backend.alert.entity.Alert;
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
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AlertResponse> getNotifications(UserPrincipal userPrincipal) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userPrincipal.getId()).stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(String alertId, UserPrincipal userPrincipal) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Alert not found"));
        if (!alert.getUser().getId().equals(userPrincipal.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "You do not have access to this alert");
        }
        alert.markAsRead();
    }

    @Transactional
    public AlertResponse createAlert(CreateAlertRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        Alert alert = alertRepository.save(new Alert(
                user,
                request.title(),
                request.message(),
                request.type() == null ? AlertType.GENERAL : request.type(),
                request.isRead() != null && request.isRead()
        ));

        return AlertResponse.from(alert);
    }
}
