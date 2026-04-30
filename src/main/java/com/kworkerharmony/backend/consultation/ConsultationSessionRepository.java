package com.kworkerharmony.backend.consultation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, String> {

    List<ConsultationSession> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
}
