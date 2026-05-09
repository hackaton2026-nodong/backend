package com.kworkerharmony.backend.document;

import com.kworkerharmony.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "document_anchors",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_anchors_chain_contract_anchor",
                columnNames = {"chain_id", "contract_address", "anchor_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAnchor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "signature_id", nullable = false, length = 36)
    private String signatureId;

    @Column(name = "chain_id", nullable = false)
    private Long chainId;

    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;

    @Column(name = "anchor_id", nullable = false, length = 66)
    private String anchorId;

    @Column(name = "document_hash", nullable = false, length = 66)
    private String documentHash;

    @Column(name = "case_id_hash", nullable = false, length = 66)
    private String caseIdHash;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentAnchorStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "anchored_at")
    private LocalDateTime anchoredAt;

    public DocumentAnchor(
            String documentId,
            String signatureId,
            Long chainId,
            String contractAddress,
            String anchorId,
            String documentHash,
            String caseIdHash
    ) {
        this.documentId = documentId;
        this.signatureId = signatureId;
        this.chainId = chainId;
        this.contractAddress = contractAddress;
        this.anchorId = anchorId;
        this.documentHash = documentHash;
        this.caseIdHash = caseIdHash;
        this.status = DocumentAnchorStatus.PENDING;
        this.retryCount = 0;
    }

    public void markSubmitted(String txHash) {
        this.txHash = txHash;
        this.status = DocumentAnchorStatus.PENDING;
    }

    public void markAnchored(String txHash, Long blockNumber) {
        this.txHash = txHash;
        this.blockNumber = blockNumber;
        this.status = DocumentAnchorStatus.ANCHORED;
        this.anchoredAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markFailed(String lastErrorMessage) {
        this.status = DocumentAnchorStatus.FAILED;
        this.retryCount++;
        this.lastErrorMessage = lastErrorMessage;
    }
}
