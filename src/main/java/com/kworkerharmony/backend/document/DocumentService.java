package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.document.dto.request.CreateDocumentRequest;
import com.kworkerharmony.backend.document.dto.response.DocumentResponse;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import com.kworkerharmony.backend.user.User;
import com.kworkerharmony.backend.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments() {
        return documentRepository.findAll().stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        Document document = documentRepository.save(new Document(
                request.issueDate(),
                request.expiryDate(),
                request.documentType(),
                request.rawData(),
                user
        ));

        return DocumentResponse.from(document);
    }
}
