package com.kworkerharmony.backend.checklist.entity;

import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.global.entity.BaseEntity;
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
@Table(name = "checklists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistStatus status;

    public Checklist(Case caseEntity, String title, String description, ChecklistStatus status) {
        this.caseEntity = caseEntity;
        this.title = title;
        this.description = description;
        this.status = status;
    }
}
