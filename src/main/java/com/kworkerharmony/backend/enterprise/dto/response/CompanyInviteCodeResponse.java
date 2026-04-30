package com.kworkerharmony.backend.enterprise.dto.response;

import com.kworkerharmony.backend.enterprise.CompanyInviteCode;
import java.time.LocalDateTime;

public record CompanyInviteCodeResponse(
        Long id,
        Long companyId,
        String code,
        LocalDateTime expiresAt,
        int maxUses,
        int usedCount,
        boolean active,
        String defaultRole,
        String caseId
) {

    public static CompanyInviteCodeResponse from(CompanyInviteCode inviteCode) {
        return new CompanyInviteCodeResponse(
                inviteCode.getId(),
                inviteCode.getEnterprise().getId(),
                inviteCode.getCode(),
                inviteCode.getExpiresAt(),
                inviteCode.getMaxUses(),
                inviteCode.getUsedCount(),
                inviteCode.isActive(),
                inviteCode.getDefaultRole().name(),
                inviteCode.getCaseId()
        );
    }
}
