package com.kworkerharmony.backend.enterprise.dto.response;

import com.kworkerharmony.backend.user.User;

public record CompanyUserResponse(
        Long id,
        String email,
        String name,
        String role,
        String status
) {

    public static CompanyUserResponse from(User user) {
        return new CompanyUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}
