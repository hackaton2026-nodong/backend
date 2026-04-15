package com.kworkerharmony.backend.checklist.domain;

import com.kworkerharmony.backend.checklist.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistRepository extends JpaRepository<Checklist, String> {

    long countByCaseEntityWorkerId(Long workerId);

    long countByCaseEntityWorkerIdAndStatus(Long workerId, ChecklistStatus status);

    long countByCaseEntityEmployerId(Long employerId);

    long countByCaseEntityEmployerIdAndStatus(Long employerId, ChecklistStatus status);
}
