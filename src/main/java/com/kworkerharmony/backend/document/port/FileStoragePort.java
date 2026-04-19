package com.kworkerharmony.backend.document.port;

import org.springframework.web.multipart.MultipartFile;

public interface FileStoragePort {

    StoredFile store(String caseId, String documentId, MultipartFile file);

    record StoredFile(
            String originalFileName,
            String storageKey,
            String mimeType,
            Long fileSize,
            String absolutePath
    ) {
    }
}
