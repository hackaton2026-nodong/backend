package com.kworkerharmony.backend.checklist.domain;

import com.kworkerharmony.backend.checklist.entity.CaseChecklistStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseChecklistStatusRepository extends JpaRepository<CaseChecklistStatus, String> {

    List<CaseChecklistStatus> findByCaseEntityId(String caseId);

    Optional<CaseChecklistStatus> findByCaseEntityIdAndChecklistItemCode(String caseId, String checklistItemCode);

    long countByCaseEntityWorkerId(Long workerId);

    long countByCaseEntityWorkerIdAndStatus(Long workerId, ChecklistStatus status);

    long countByCaseEntityEmployerId(Long employerId);

    long countByCaseEntityEmployerIdAndStatus(Long employerId, ChecklistStatus status);
}
