package com.kworkerharmony.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kworkerharmony.backend.ai.dto.request.AiChatStreamRequest;
import com.kworkerharmony.backend.document.config.DocumentAiProperties;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiChatStreamService {

    private static final String INTERNAL_TOKEN_HEADER = "X-AI-Internal-Token";
    private static final Set<String> FORBIDDEN_MESSAGE_HINTS = Set.of(
            "rawocrtext",
            "raw_ocr_text",
            "layoutparsingresults",
            "parsing_res_list",
            "block_content",
            "base64",
            "imagebytes",
            "filebytes"
    );

    private final DocumentAiProperties properties;
    private final ObjectMapper objectMapper;

    public AiChatStreamService(DocumentAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public SseEmitter stream(AiChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(properties.readTimeoutMillis());
        if (!isSafeRequest(request)) {
            sendErrorEvent(emitter, "Raw OCR or file payload is not allowed in chat.");
            return emitter;
        }
        CompletableFuture.runAsync(() -> proxy(request, emitter));
        return emitter;
    }

    public JsonNode uploadAttachment(MultipartFile file) {
        if (!properties.enabled() || properties.chatStreamEndpoint().isBlank()) {
            throw new IllegalStateException("AI chat attachment upload is disabled.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file is required.");
        }
        try {
            String boundary = "kohamo-chat-" + UUID.randomUUID();
            HttpResponse<String> response = httpClient().send(
                    attachmentRequest(file, boundary),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI chat attachment upload returned HTTP " + response.statusCode() + ".");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("AI chat attachment upload failed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI chat attachment upload was interrupted.", ex);
        }
    }

    private boolean isSafeRequest(AiChatStreamRequest request) {
        if (containsForbiddenHint(request.message())) {
            return false;
        }
        if (request.history() != null && request.history().stream()
                .map(AiChatStreamRequest.ChatHistoryMessage::content)
                .anyMatch(this::containsForbiddenHint)) {
            return false;
        }
        return isSafeCaseContext(request.caseContext())
                && isSafeAttachments(request.attachments());
    }

    private boolean isSafeCaseContext(AiChatStreamRequest.CaseContext context) {
        if (context == null) {
            return true;
        }
        if (containsForbiddenHint(context.documentId())
                || containsForbiddenHint(context.documentStatus())
                || containsForbiddenHint(context.riskLevel())
                || containsForbiddenHint(context.contractPeriod())
                || containsForbiddenHint(context.contractTerms())
                || containsForbiddenHint(context.analysisStatus())
                || containsForbiddenHint(context.analysisSummary())
                || containsForbiddenHint(context.generatedAnalysisText())) {
            return false;
        }
        return isSafeStringList(context.issueCandidates())
                && isSafeRiskFlags(context.riskFlags())
                && isSafeFindings(context.findings())
                && isSafeRecommendedActions(context.recommendedActions());
    }

    private boolean isSafeStringList(java.util.List<String> values) {
        return values == null || values.stream().noneMatch(this::containsForbiddenHint);
    }

    private boolean isSafeRiskFlags(java.util.List<AiChatStreamRequest.RiskFlagContext> values) {
        return values == null || values.stream().noneMatch(value ->
                containsForbiddenHint(value.code())
                        || containsForbiddenHint(value.label())
                        || containsForbiddenHint(value.level())
                        || containsForbiddenHint(value.description()));
    }

    private boolean isSafeFindings(java.util.List<AiChatStreamRequest.FindingContext> values) {
        return values == null || values.stream().noneMatch(value ->
                containsForbiddenHint(value.id())
                        || containsForbiddenHint(value.title())
                        || containsForbiddenHint(value.description())
                        || containsForbiddenHint(value.severity())
                        || containsForbiddenHint(value.fieldName()));
    }

    private boolean isSafeRecommendedActions(java.util.List<AiChatStreamRequest.RecommendedActionContext> values) {
        return values == null || values.stream().noneMatch(value ->
                containsForbiddenHint(value.label())
                        || containsForbiddenHint(value.description())
                        || containsForbiddenHint(value.priority())
                        || containsForbiddenHint(value.institutionName())
                        || containsForbiddenHint(value.expectedPath()));
    }

    private boolean isSafeAttachments(java.util.List<AiChatStreamRequest.AttachmentContext> values) {
        return values == null || values.stream().noneMatch(value ->
                containsForbiddenHint(value.attachmentId())
                        || containsForbiddenHint(value.fileName())
                        || containsForbiddenHint(value.mimeType())
                        || containsForbiddenHint(value.textPreview())
                        || containsForbiddenHint(value.status())
                        || containsForbiddenHint(value.warning()));
    }

    private boolean containsForbiddenHint(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return FORBIDDEN_MESSAGE_HINTS.stream().anyMatch(normalized::contains);
    }

    private boolean containsForbiddenHint(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return false;
        }
        return containsForbiddenHint(node.toString());
    }

    private void proxy(AiChatStreamRequest request, SseEmitter emitter) {
        if (!properties.enabled() || properties.chatStreamEndpoint().isBlank()) {
            sendErrorEvent(emitter, "AI chat stream is disabled.");
            return;
        }
        try {
            HttpResponse<java.io.InputStream> response = httpClient().send(
                    httpRequest(request),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                sendErrorEvent(emitter, "AI chat stream returned HTTP " + response.statusCode() + ".");
                return;
            }
            forwardSse(response, emitter);
            emitter.complete();
        } catch (IOException ex) {
            sendErrorEvent(emitter, "AI chat stream connection failed.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            sendErrorEvent(emitter, "AI chat stream was interrupted.");
        } catch (RuntimeException ex) {
            sendErrorEvent(emitter, "AI chat stream failed.");
        }
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()))
                .build();
    }

    private HttpRequest httpRequest(AiChatStreamRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.chatStreamEndpoint()))
                .timeout(Duration.ofMillis(properties.readTimeoutMillis()))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(request)));
        if (!properties.internalToken().isBlank()) {
            builder.header(INTERNAL_TOKEN_HEADER, properties.internalToken());
        }
        return builder.build();
    }

    private HttpRequest attachmentRequest(MultipartFile file, String boundary) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(attachmentEndpoint())
                .timeout(Duration.ofMillis(properties.readTimeoutMillis()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(file, boundary)));
        if (!properties.internalToken().isBlank()) {
            builder.header(INTERNAL_TOKEN_HEADER, properties.internalToken());
        }
        return builder.build();
    }

    private URI attachmentEndpoint() {
        String endpoint = properties.chatStreamEndpoint();
        if (endpoint.endsWith("/stream")) {
            return URI.create(endpoint.substring(0, endpoint.length() - "/stream".length()) + "/attachments");
        }
        return URI.create(endpoint.replaceAll("/+$", "") + "/attachments");
    }

    private byte[] multipartBody(MultipartFile file, String boundary) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeMultipartText(output, "--" + boundary + "\r\n");
        writeMultipartText(output, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFileName(file.getOriginalFilename()) + "\"\r\n");
        writeMultipartText(output, "Content-Type: " + safeContentType(file.getContentType()) + "\r\n\r\n");
        output.write(file.getBytes());
        writeMultipartText(output, "\r\n--" + boundary + "--\r\n");
        return output.toByteArray();
    }

    private void writeMultipartText(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private String safeFileName(String fileName) {
        String value = fileName == null || fileName.isBlank() ? "attachment" : fileName;
        return value.replace("\\", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");
    }

    private String safeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType.replace("\r", "").replace("\n", "");
    }

    private String requestBody(AiChatStreamRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("message", request.message());
        root.put("languageCode", request.languageCode() == null || request.languageCode().isBlank()
                ? "ko"
                : request.languageCode());
        root.put("topK", request.topK() == null ? 3 : request.topK());
        ArrayNode history = root.putArray("history");
        if (request.history() != null) {
            for (AiChatStreamRequest.ChatHistoryMessage message : request.history()) {
                ObjectNode item = history.addObject();
                item.put("role", message.role());
                item.put("content", message.content());
            }
        }
        if (request.caseContext() != null) {
            ObjectNode context = root.putObject("caseContext");
            putIfPresent(context, "documentId", request.caseContext().documentId());
            putIfPresent(context, "documentStatus", request.caseContext().documentStatus());
            putIfPresent(context, "riskLevel", request.caseContext().riskLevel());
            putIfPresent(context, "contractPeriod", request.caseContext().contractPeriod());
            JsonNode contractTerms = sanitizedContractTerms(request.caseContext().contractTerms());
            if (!contractTerms.isMissingNode()) {
                context.set("contractTerms", contractTerms);
            }
            putIfPresent(context, "analysisStatus", request.caseContext().analysisStatus());
            putIfPresent(context, "analysisSummary", request.caseContext().analysisSummary());
            putIfPresent(context, "generatedAnalysisText", request.caseContext().generatedAnalysisText());
            ArrayNode issueCandidates = context.putArray("issueCandidates");
            if (request.caseContext().issueCandidates() != null) {
                request.caseContext().issueCandidates().forEach(issueCandidates::add);
            }
            ArrayNode riskFlags = context.putArray("riskFlags");
            if (request.caseContext().riskFlags() != null) {
                request.caseContext().riskFlags().forEach(flag -> {
                    ObjectNode item = riskFlags.addObject();
                    putIfPresent(item, "code", flag.code());
                    putIfPresent(item, "label", flag.label());
                    putIfPresent(item, "level", flag.level());
                    putIfPresent(item, "description", flag.description());
                });
            }
            ArrayNode findings = context.putArray("findings");
            if (request.caseContext().findings() != null) {
                request.caseContext().findings().forEach(finding -> {
                    ObjectNode item = findings.addObject();
                    putIfPresent(item, "id", finding.id());
                    putIfPresent(item, "title", finding.title());
                    putIfPresent(item, "description", finding.description());
                    putIfPresent(item, "severity", finding.severity());
                    putIfPresent(item, "fieldName", finding.fieldName());
                });
            }
            ArrayNode recommendedActions = context.putArray("recommendedActions");
            if (request.caseContext().recommendedActions() != null) {
                request.caseContext().recommendedActions().forEach(action -> {
                    ObjectNode item = recommendedActions.addObject();
                    putIfPresent(item, "label", action.label());
                    putIfPresent(item, "description", action.description());
                    putIfPresent(item, "priority", action.priority());
                    putIfPresent(item, "institutionName", action.institutionName());
                    putIfPresent(item, "expectedPath", action.expectedPath());
                });
            }
        }
        ArrayNode attachments = root.putArray("attachments");
        if (request.attachments() != null) {
            request.attachments().forEach(attachment -> {
                ObjectNode item = attachments.addObject();
                putIfPresent(item, "attachmentId", attachment.attachmentId());
                putIfPresent(item, "fileName", attachment.fileName());
                putIfPresent(item, "mimeType", attachment.mimeType());
                if (attachment.fileSize() != null) {
                    item.put("fileSize", attachment.fileSize());
                }
                putIfPresent(item, "textPreview", attachment.textPreview());
                putIfPresent(item, "status", attachment.status());
                putIfPresent(item, "warning", attachment.warning());
            });
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid chat stream request", ex);
        }
    }

    private void putIfPresent(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value);
        }
    }

    private void forwardSse(HttpResponse<java.io.InputStream> response, SseEmitter emitter) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String eventName = "message";
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    if (!data.isEmpty()) {
                        sendEvent(emitter, eventName, data.toString());
                    }
                    eventName = "message";
                    data.setLength(0);
                    continue;
                }
                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }
            if (!data.isEmpty()) {
                sendEvent(emitter, eventName, data.toString());
            }
        }
    }

    private JsonNode sanitizedContractTerms(JsonNode terms) {
        if (terms == null || !terms.isObject()) {
            return MissingNode.getInstance();
        }
        ObjectNode sanitized = objectMapper.createObjectNode();
        copyObjectFields(sanitized, terms, "contractPeriod", Set.of(
                "contractStartDate", "contractEndDate"
        ));
        copyObjectFields(sanitized, terms, "wage", Set.of(
                "amount", "basePay", "currency", "period", "paymentDay", "paymentMethod",
                "overtimeNightHolidayPremiumMentioned"
        ));
        copyObjectFields(sanitized, terms, "workingHours", Set.of(
                "startTime", "endTime", "hoursPerDay", "hoursPerWeek", "overtimeHoursPerDay",
                "maxVariableHoursPerDay"
        ));
        copyObjectFields(sanitized, terms, "breakTime", Set.of("minutesPerDay"));
        copyObjectFields(sanitized, terms, "holidays", Set.of("legalHolidayPaid", "otherHoliday"));
        copyObjectFields(sanitized, terms, "dormitory", Set.of("provided", "typeCategory", "deductionAmount"));
        copyObjectFields(sanitized, terms, "meals", Set.of("provided", "deductionAmount", "providedMeals"));
        copyObjectFields(sanitized, terms, "work", Set.of("industryCategory", "jobCategory", "workplaceRegion"));
        return sanitized.isEmpty() ? MissingNode.getInstance() : sanitized;
    }

    private void copyObjectFields(ObjectNode target, JsonNode source, String objectName, Set<String> allowedFields) {
        JsonNode nested = source.path(objectName);
        if (!nested.isObject()) {
            return;
        }
        ObjectNode safeNested = objectMapper.createObjectNode();
        allowedFields.forEach(field -> {
            JsonNode value = nested.get(field);
            if (value == null || value.isNull() || value.isMissingNode()) {
                return;
            }
            if (value.isTextual()) {
                String text = value.asText();
                safeNested.put(field, text.length() > 160 ? text.substring(0, 160) : text);
            } else if (value.isNumber() || value.isBoolean()) {
                safeNested.set(field, value);
            } else if (value.isArray() && "providedMeals".equals(field)) {
                ArrayNode safeArray = safeNested.putArray(field);
                value.forEach(item -> {
                    if (item.isTextual() && safeArray.size() < 6) {
                        String text = item.asText();
                        safeArray.add(text.length() > 80 ? text.substring(0, 80) : text);
                    }
                });
            }
        });
        if (!safeNested.isEmpty()) {
            target.set(objectName, safeNested);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", message);
            sendEvent(emitter, "error", objectMapper.writeValueAsString(error));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return;
        }
        emitter.complete();
    }
}
