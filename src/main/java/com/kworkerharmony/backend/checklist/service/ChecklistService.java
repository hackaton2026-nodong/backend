package com.kworkerharmony.backend.checklist.service;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.checklist.domain.CaseChecklistItemRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistItemRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.domain.dto.request.CreateChecklistRequest;
import com.kworkerharmony.backend.checklist.domain.dto.request.CreateChecklistItemRequest;
import com.kworkerharmony.backend.checklist.domain.dto.response.ChecklistItemResponse;
import com.kworkerharmony.backend.checklist.domain.dto.response.ChecklistResponse;
import com.kworkerharmony.backend.checklist.entity.CaseChecklistItem;
import com.kworkerharmony.backend.checklist.entity.ChecklistItem;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final CaseChecklistItemRepository caseChecklistItemRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final CaseRepository caseRepository;

    @Transactional(readOnly = true)
    public List<ChecklistResponse> getChecklists(String caseId) {
        return caseChecklistItemRepository.findByCaseEntityIdOrderByCreatedAtAsc(caseId).stream()
                .map(ChecklistResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistResponse getChecklist(String checklistId) {
        CaseChecklistItem checklist = caseChecklistItemRepository.findById(checklistId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist not found"));
        return ChecklistResponse.from(checklist);
    }

    @Transactional
    public ChecklistResponse createChecklist(CreateChecklistRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));
        ChecklistItem checklistItem = checklistItemRepository.findById(request.checklistItemId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist item not found"));

        CaseChecklistItem checklist = caseChecklistItemRepository.save(new CaseChecklistItem(
                caseEntity,
                checklistItem,
                request.status() == null ? ChecklistStatus.NOT_STARTED : request.status(),
                request.note()
        ));

        return ChecklistResponse.from(checklist);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getChecklistItems() {
        return checklistItemRepository.findAll().stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    @Transactional
    public ChecklistItemResponse createChecklistItem(CreateChecklistItemRequest request) {
        checklistItemRepository.findByCode(request.code())
                .ifPresent(item -> {
                    throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "Checklist item code already exists");
                });

        ChecklistItem checklistItem = checklistItemRepository.save(new ChecklistItem(
                request.code(),
                request.title(),
                request.description(),
                request.required()
        ));

        return ChecklistItemResponse.from(checklistItem);
    }
}
