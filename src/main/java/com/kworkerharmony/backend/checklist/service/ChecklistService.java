package com.kworkerharmony.backend.checklist.service;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.checklist.domain.CaseChecklistStatusRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.domain.dto.request.CreateChecklistRequest;
import com.kworkerharmony.backend.checklist.domain.dto.response.ChecklistItemResponse;
import com.kworkerharmony.backend.checklist.domain.dto.response.ChecklistResponse;
import com.kworkerharmony.backend.checklist.entity.CaseChecklistStatus;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.reference.checklist.ChecklistCatalog;
import com.kworkerharmony.backend.reference.checklist.ChecklistItemDefinition;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final CaseChecklistStatusRepository caseChecklistStatusRepository;
    private final CaseRepository caseRepository;
    private final ChecklistCatalog checklistCatalog;

    @Transactional(readOnly = true)
    public List<ChecklistResponse> getChecklists(String caseId) {
        caseRepository.findById(caseId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));

        Map<String, CaseChecklistStatus> statusByCode = caseChecklistStatusRepository.findByCaseEntityId(caseId).stream()
                .collect(Collectors.toMap(CaseChecklistStatus::getChecklistItemCode, Function.identity()));

        return checklistCatalog.getItems().stream()
                .map(definition -> ChecklistResponse.from(
                        caseId,
                        definition,
                        statusByCode.get(definition.code())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistResponse getChecklist(String checklistId) {
        CaseChecklistStatus checklist = caseChecklistStatusRepository.findById(checklistId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist not found"));
        ChecklistItemDefinition definition = checklistCatalog.findItem(checklist.getChecklistItemCode())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist definition not found"));
        return ChecklistResponse.from(checklist, definition);
    }

    @Transactional
    public ChecklistResponse createChecklist(CreateChecklistRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        String checklistItemCode = normalizedChecklistItemCode(request.checklistItemCode());
        ChecklistItemDefinition definition = checklistCatalog.findItem(checklistItemCode)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist item not found"));

        CaseChecklistStatus checklist = caseChecklistStatusRepository
                .findByCaseEntityIdAndChecklistItemCode(caseEntity.getId(), checklistItemCode)
                .map(existing -> {
                    existing.update(
                            request.status() == null ? ChecklistStatus.NOT_STARTED : request.status(),
                            request.note()
                    );
                    return existing;
                })
                .orElseGet(() -> caseChecklistStatusRepository.save(new CaseChecklistStatus(
                        caseEntity,
                        checklistItemCode,
                        request.status() == null ? ChecklistStatus.NOT_STARTED : request.status(),
                        request.note()
                )));

        return ChecklistResponse.from(checklist, definition);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getChecklistItems() {
        return checklistCatalog.getItems().stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    private String normalizedChecklistItemCode(String checklistItemCode) {
        return checklistCatalog.normalize(checklistItemCode);
    }
}
