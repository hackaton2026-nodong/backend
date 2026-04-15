package com.kworkerharmony.backend.checklist.service;

import com.kworkerharmony.backend.cases.domain.CaseRepository;
import com.kworkerharmony.backend.cases.entity.Case;
import com.kworkerharmony.backend.checklist.domain.ChecklistRepository;
import com.kworkerharmony.backend.checklist.domain.ChecklistStatus;
import com.kworkerharmony.backend.checklist.domain.dto.request.CreateChecklistRequest;
import com.kworkerharmony.backend.checklist.domain.dto.response.ChecklistResponse;
import com.kworkerharmony.backend.checklist.entity.Checklist;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final CaseRepository caseRepository;

    @Transactional(readOnly = true)
    public List<ChecklistResponse> getChecklists() {
        return checklistRepository.findAll().stream()
                .map(ChecklistResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistResponse getChecklist(String checklistId) {
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist not found"));
        return ChecklistResponse.from(checklist);
    }

    @Transactional
    public ChecklistResponse createChecklist(CreateChecklistRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Case not found"));

        Checklist checklist = checklistRepository.save(new Checklist(
                caseEntity,
                request.title(),
                request.description(),
                request.status() == null ? ChecklistStatus.PENDING : request.status()
        ));

        return ChecklistResponse.from(checklist);
    }
}
