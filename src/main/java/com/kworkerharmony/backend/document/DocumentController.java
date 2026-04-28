package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.document.dto.request.AnchorDocumentRequest;
import com.kworkerharmony.backend.document.dto.request.CorrectDocumentExtractionRequest;
import com.kworkerharmony.backend.document.dto.request.CreatePaddleOcrExtractionRequest;
import com.kworkerharmony.backend.document.dto.request.ReceivePaddleOcrResultRequest;
import com.kworkerharmony.backend.document.dto.request.SubmitDocumentSignatureRequest;
import com.kworkerharmony.backend.document.dto.response.DocumentAnalysisResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentAnchorResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentExtractionResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentSignatureRequestResponse;
import com.kworkerharmony.backend.document.dto.response.DocumentSignatureResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @GetMapping("/documents/{documentId}/signature-request")
    public ApiResponse<DocumentSignatureRequestResponse> createSignatureRequest(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.createSignatureRequest(documentId, userPrincipal));
    }

    @PostMapping("/documents/{documentId}/signatures")
    public ApiResponse<DocumentSignatureResponse> submitSignature(
            @PathVariable String documentId,
            @Valid @RequestBody SubmitDocumentSignatureRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.submitSignature(documentId, request, userPrincipal));
    }

    @PostMapping("/documents/{documentId}/anchor")
    public ApiResponse<DocumentAnchorResponse> anchorDocument(
            @PathVariable String documentId,
            @Valid @RequestBody AnchorDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.anchorDocument(documentId, request, userPrincipal));
    }

    @GetMapping("/documents/{documentId}/anchor")
    public ApiResponse<DocumentAnchorResponse> getAnchor(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.getAnchor(documentId, userPrincipal));
    }

    @PostMapping("/documents/{documentId}/analysis")
    public ApiResponse<DocumentAnalysisResponse> analyzeDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.analyzeDocument(documentId, userPrincipal));
    }

    @GetMapping("/documents/{documentId}/analysis")
    public ApiResponse<DocumentAnalysisResponse> getAnalysis(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.getAnalysis(documentId, userPrincipal));
    }

    @PostMapping("/documents/{documentId}/extraction/paddle-ocr")
    public ApiResponse<DocumentExtractionResponse> createPaddleOcrExtraction(
            @PathVariable String documentId,
            @Valid @RequestBody CreatePaddleOcrExtractionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.createPaddleOcrExtraction(documentId, request, userPrincipal));
    }

    @GetMapping("/documents/{documentId}/extraction")
    public ApiResponse<DocumentExtractionResponse> getExtraction(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.getExtraction(documentId, userPrincipal));
    }

    @PutMapping("/documents/{documentId}/extraction/correction")
    public ApiResponse<DocumentExtractionResponse> correctExtraction(
            @PathVariable String documentId,
            @Valid @RequestBody CorrectDocumentExtractionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(documentService.correctExtraction(documentId, request, userPrincipal));
    }

    @PostMapping("/internal/documents/{documentId}/ocr-result")
    public ApiResponse<DocumentExtractionResponse> receivePaddleOcrResult(
            @PathVariable String documentId,
            @Valid @RequestBody ReceivePaddleOcrResultRequest request,
            @RequestHeader(name = "X-OCR-Callback-Token", required = false) String callbackToken
    ) {
        return ApiResponse.success(documentService.receivePaddleOcrResult(documentId, request, callbackToken));
    }
}
