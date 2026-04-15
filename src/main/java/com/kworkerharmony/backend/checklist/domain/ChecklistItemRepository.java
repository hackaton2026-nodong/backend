package com.kworkerharmony.backend.checklist.domain;

import com.kworkerharmony.backend.checklist.entity.ChecklistItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, String> {

    Optional<ChecklistItem> findByCode(String code);
}
