package com.kworkerharmony.backend.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferenceResourceReader {

    private final ObjectMapper objectMapper;

    public <T> T read(String resourcePath, Class<T> type) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return objectMapper.readValue(inputStream, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load reference resource: " + resourcePath, exception);
        }
    }
}
