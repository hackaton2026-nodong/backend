package com.kworkerharmony.backend.cases.entity;

import com.kworkerharmony.backend.cases.domain.CaseStatus;
import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.global.entity.BaseEntity;
import com.kworkerharmony.backend.user.User;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Case extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id")
    private User employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status;

    @Column(nullable = false, length = 100)
    private String industry;

    @Column(nullable = false, length = 100)
    private String region;

    public Case(Enterprise enterprise, User employer, User worker, CaseStatus status, String industry, String region) {
        this.enterprise = enterprise;
        this.employer = employer;
        this.worker = worker;
        this.status = status;
        this.industry = industry;
        this.region = region;
    }

    public void connectMembers(User employer, User worker) {
        this.employer = employer;
        this.worker = worker;
        this.status = CaseStatus.ACTIVE;
    }

    public void connectWorker(User worker) {
        this.worker = worker;
        this.status = CaseStatus.ACTIVE;
    }
}
