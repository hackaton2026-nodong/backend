package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.document.dto.request.CreateDocumentRequest;
import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
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
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ApiResponse<List<DocumentResponse>> getDocuments() {
        return ApiResponse.success(documentService.getDocuments());
    }

    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponse> getDocument(@PathVariable Long documentId) {
        return ApiResponse.success(documentService.getDocument(documentId));
    }

    @PostMapping
    public ApiResponse<DocumentResponse> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return ApiResponse.success(documentService.createDocument(request));
    }
}
