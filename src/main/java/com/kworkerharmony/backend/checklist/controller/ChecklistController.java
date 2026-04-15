package com.kworkerharmony.backend.checklist.controller;

import com.kworkerharmony.backend.checklist.domain.dto.request.CreateChecklistRequest;
import com.kworkerharmony.backend.checklist.domain.dto.response.ChecklistResponse;
import com.kworkerharmony.backend.checklist.service.ChecklistService;
import com.kworkerharmony.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    public ApiResponse<List<ChecklistResponse>> getChecklists() {
        return ApiResponse.success(checklistService.getChecklists());
    }

    @GetMapping("/{checklistId}")
    public ApiResponse<ChecklistResponse> getChecklist(@PathVariable String checklistId) {
        return ApiResponse.success(checklistService.getChecklist(checklistId));
    }

    @PostMapping
    public ApiResponse<ChecklistResponse> createChecklist(@Valid @RequestBody CreateChecklistRequest request) {
        return ApiResponse.success(checklistService.createChecklist(request));
    }
}
