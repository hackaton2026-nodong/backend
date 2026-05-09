package com.kworkerharmony.backend.alert.domain;

import com.kworkerharmony.backend.alert.entity.Alert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, String> {

    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
