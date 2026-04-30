package com.kworkerharmony.backend.consultation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, String> {

    List<ConsultationMessage> findAllBySessionIdOrderByCreatedAtAsc(String sessionId);

    Optional<ConsultationMessage> findFirstBySessionIdOrderByCreatedAtDesc(String sessionId);
}
