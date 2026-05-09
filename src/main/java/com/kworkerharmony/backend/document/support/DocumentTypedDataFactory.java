package com.kworkerharmony.backend.document.support;

import com.kworkerharmony.backend.document.Document;
import com.kworkerharmony.backend.document.config.DocumentBlockchainProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypedDataFactory {

    private static final String PRIMARY_TYPE = "DocumentConsent";

    public Map<String, Object> domain(DocumentBlockchainProperties properties) {
        return Map.of(
                "name", properties.domainName(),
                "version", properties.domainVersion(),
                "chainId", properties.chainId(),
                "verifyingContract", properties.contractAddress()
        );
    }

    public Map<String, Object> types() {
        return Map.of(
                PRIMARY_TYPE,
                List.of(
                        Map.of("name", "documentId", "type", "string"),
                        Map.of("name", "caseId", "type", "string"),
                        Map.of("name", "documentHash", "type", "bytes32"),
                        Map.of("name", "documentType", "type", "string"),
                        Map.of("name", "signerUserId", "type", "uint256"),
                        Map.of("name", "nonce", "type", "bytes32"),
                        Map.of("name", "deadline", "type", "uint256")
                )
        );
    }

    public Map<String, Object> message(Document document, Long userId, String nonce, LocalDateTime deadline) {
        return Map.of(
                "documentId", document.getId(),
                "caseId", document.getCaseId(),
                "documentHash", DocumentCrypto.ensureBytes32Hex(document.getSha256Hash()),
                "documentType", document.getDocumentType(),
                "signerUserId", userId,
                "nonce", nonce,
                "deadline", deadline.toEpochSecond(ZoneOffset.UTC)
        );
    }

    public String typedDataHash(
            DocumentBlockchainProperties properties,
            Document document,
            Long userId,
            String nonce,
            LocalDateTime deadline
    ) {
        String canonical = "name=" + properties.domainName()
                + "|version=" + properties.domainVersion()
                + "|chainId=" + properties.chainId()
                + "|verifyingContract=" + properties.contractAddress().toLowerCase()
                + "|documentId=" + document.getId()
                + "|caseId=" + document.getCaseId()
                + "|documentHash=" + DocumentCrypto.ensureBytes32Hex(document.getSha256Hash()).toLowerCase()
                + "|documentType=" + document.getDocumentType()
                + "|signerUserId=" + userId
                + "|nonce=" + nonce.toLowerCase()
                + "|deadline=" + deadline.toEpochSecond(ZoneOffset.UTC);
        return DocumentCrypto.bytes32HexFromSha256(canonical);
    }

    public Instant deadlineInstant(LocalDateTime deadline) {
        return deadline.toInstant(ZoneOffset.UTC);
    }
}
