package com.kworkerharmony.backend.document.infrastructure;

import com.kworkerharmony.backend.document.port.FileStoragePort;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path rootDirectory;

    public LocalFileStorageAdapter(@Value("${app.document.storage-root:/tmp/backend-documents}") String storageRoot) {
        this.rootDirectory = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(String caseId, String documentId, MultipartFile file) {
        try {
            Path directory = rootDirectory.resolve(caseId);
            Files.createDirectories(directory);

            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : documentId;
            Path targetFile = directory.resolve(documentId + "-" + originalFileName).normalize();
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile(
                    originalFileName,
                    rootDirectory.relativize(targetFile).toString().replace(File.separatorChar, '/'),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    file.getSize(),
                    targetFile.toString()
            );
        } catch (IOException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        return Files.exists(rootDirectory.resolve(storageKey.replace('\\', File.separatorChar)).normalize());
    }
}
