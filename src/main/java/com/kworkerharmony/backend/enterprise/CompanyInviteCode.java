package com.kworkerharmony.backend.enterprise;

import com.kworkerharmony.backend.global.entity.BaseEntity;
import com.kworkerharmony.backend.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "company_invite_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyInviteCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role defaultRole;

    public CompanyInviteCode(
            Enterprise enterprise,
            String code,
            LocalDateTime expiresAt,
            int maxUses,
            int usedCount,
            boolean active,
            Role defaultRole
    ) {
        this.enterprise = enterprise;
        this.code = code;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.usedCount = usedCount;
        this.active = active;
        this.defaultRole = defaultRole;
    }

    public void use() {
        this.usedCount += 1;
    }

    public boolean isUsableAt(LocalDateTime now) {
        return active && usedCount < maxUses && !expiresAt.isBefore(now);
    }
}
