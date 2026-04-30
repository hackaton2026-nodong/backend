package com.kworkerharmony.backend.reference.checklist;

public record ChecklistItemDefinition(
        String sectionCode,
        String sectionTitle,
        String code,
        String title,
        String description,
        boolean required,
        int displayOrder,
        ChecklistTriggerType triggerType
) {

    public ChecklistTriggerType triggerTypeOrDefault() {
        return triggerType == null ? ChecklistTriggerType.AD_HOC : triggerType;
    }
}
