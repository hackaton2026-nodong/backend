package com.kworkerharmony.backend.cases.domain;

import com.kworkerharmony.backend.cases.entity.Case;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<Case, String> {

    @Query("""
            select c from Case c
            where c.status = :status
            and (c.employer.id = :userId or c.worker.id = :userId)
            order by c.createdAt desc
            """)
    List<Case> findActiveCasesByUserId(Long userId, CaseStatus status);

    long countByEmployerIdAndStatus(Long employerId, CaseStatus status);

    long countByWorkerIdAndStatus(Long workerId, CaseStatus status);
}
