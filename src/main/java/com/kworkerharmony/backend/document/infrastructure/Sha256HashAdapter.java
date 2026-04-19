package com.kworkerharmony.backend.document.infrastructure;

import com.kworkerharmony.backend.document.port.DocumentHashPort;
import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class Sha256HashAdapter implements DocumentHashPort {

    @Override
    public String hash(String absolutePath) {
        try (InputStream inputStream = Files.newInputStream(Path.of(absolutePath))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return toHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to hash document");
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
