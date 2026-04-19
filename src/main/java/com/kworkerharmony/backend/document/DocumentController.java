package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/cases/{caseId}/documents")
    public ApiResponse<List<DocumentResponse>> getDocuments(
            @PathVariable String caseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.getDocuments(caseId, userPrincipal));
    }

    @GetMapping("/documents/{documentId}")
    public ApiResponse<DocumentResponse> getDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.getDocument(documentId, userPrincipal));
    }

    @PostMapping("/cases/{caseId}/documents")
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable String caseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam DocumentType documentType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresAt,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.uploadDocument(
                caseId,
                file,
                documentType,
                issuedAt,
                expiresAt,
                userPrincipal
        ));
    }
}
