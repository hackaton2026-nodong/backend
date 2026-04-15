package com.kworkerharmony.backend.checklist.domain;

import com.kworkerharmony.backend.checklist.entity.CaseChecklistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseChecklistItemRepository extends JpaRepository<CaseChecklistItem, String> {

    List<CaseChecklistItem> findByCaseEntityIdOrderByCreatedAtAsc(String caseId);

    long countByCaseEntityWorkerId(Long workerId);

    long countByCaseEntityWorkerIdAndStatus(Long workerId, ChecklistStatus status);

    long countByCaseEntityEmployerId(Long employerId);

    long countByCaseEntityEmployerIdAndStatus(Long employerId, ChecklistStatus status);
}
