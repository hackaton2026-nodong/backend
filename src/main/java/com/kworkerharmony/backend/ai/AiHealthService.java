package com.kworkerharmony.backend.ai;

import com.kworkerharmony.backend.ai.dto.response.AiHealthResponse;
import com.kworkerharmony.backend.document.config.DocumentAnalysisAiProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiHealthService {

    private final DocumentAnalysisAiProperties properties;

    public AiHealthResponse getHealth() {
        if (!properties.enabled()) {
            return new AiHealthResponse(
                    "STUB",
                    "AVAILABLE",
                    true,
                    properties.healthPath(),
                    null,
                    "AI analysis uses the local stub adapter.",
                    LocalDateTime.now()
            );
        }

        try {
            URI healthUri = properties.healthUri();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.healthTimeoutMillis()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(healthUri)
                    .timeout(Duration.ofMillis(properties.healthTimeoutMillis()))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            boolean available = response.statusCode() >= 200 && response.statusCode() < 300;
            return new AiHealthResponse(
                    "HTTP",
                    available ? "AVAILABLE" : "UNAVAILABLE",
                    available,
                    properties.healthPath(),
                    response.statusCode(),
                    available ? "AI server health check succeeded." : "AI server returned a non-2xx health status.",
                    LocalDateTime.now()
            );
        } catch (IOException ex) {
            return unavailable("AI server health check failed.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return unavailable("AI server health check was interrupted.");
        } catch (IllegalArgumentException ex) {
            return unavailable("AI server health check configuration is invalid.");
        }
    }

    private AiHealthResponse unavailable(String message) {
        return new AiHealthResponse(
                "HTTP",
                "UNAVAILABLE",
                false,
                properties.healthPath(),
                null,
                message,
                LocalDateTime.now()
        );
    }
}
