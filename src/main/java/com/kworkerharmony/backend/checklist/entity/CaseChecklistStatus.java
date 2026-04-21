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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "case_checklist_statuses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_case_checklist_statuses_case_item_code",
                columnNames = {"case_id", "checklist_item_code"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseChecklistStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(name = "checklist_item_code", nullable = false, length = 100)
    private String checklistItemCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistStatus status;

    @Column(length = 1000)
    private String note;

    public CaseChecklistStatus(Case caseEntity, String checklistItemCode, ChecklistStatus status, String note) {
        this.caseEntity = caseEntity;
        this.checklistItemCode = checklistItemCode;
        this.status = status;
        this.note = note;
    }

    public void update(ChecklistStatus status, String note) {
        this.status = status;
        this.note = note;
    }
}
