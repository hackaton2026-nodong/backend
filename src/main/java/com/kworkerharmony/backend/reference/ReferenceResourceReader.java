package com.kworkerharmony.backend.reference;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
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

    public <T> List<T> readList(String resourcePath, Class<T> elementType) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return Collections.unmodifiableList(objectMapper.readValue(inputStream, listType));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load reference resource: " + resourcePath, exception);
        }
    }
}
