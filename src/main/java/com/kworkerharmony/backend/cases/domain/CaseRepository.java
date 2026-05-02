package com.kworkerharmony.backend.cases.domain;

import com.kworkerharmony.backend.cases.entity.Case;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    @Query("""
            select c from Case c
            where c.status in :statuses
            and (c.employer.id = :userId or c.worker.id = :userId)
            order by c.createdAt desc
            """)
    List<Case> findVisibleCasesByUserId(Long userId, Collection<CaseStatus> statuses);

    long countByEmployerIdAndStatus(Long employerId, CaseStatus status);

    long countByEmployerIdAndStatusIn(Long employerId, Collection<CaseStatus> statuses);

    long countByWorkerIdAndStatus(Long workerId, CaseStatus status);

    Optional<Case> findFirstByWorkerIdAndStatusOrderByCreatedAtDesc(Long workerId, CaseStatus status);
}
