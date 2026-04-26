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
        name = "document_signatures",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_signatures_document_user_wallet",
                        columnNames = {"document_id", "user_id", "wallet_address"}
                ),
                @UniqueConstraint(
                        name = "uk_document_signatures_chain_contract_nonce",
                        columnNames = {"chain_id", "verifying_contract", "nonce"}
                ),
                @UniqueConstraint(
                        name = "uk_document_signatures_typed_data_hash",
                        columnNames = "typed_data_hash"
                ),
                @UniqueConstraint(
                        name = "uk_document_signatures_signature_hash",
                        columnNames = "signature_hash"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentSignature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_address", length = 42)
    private String walletAddress;

    @Column(name = "chain_id", nullable = false)
    private Long chainId;

    @Column(name = "verifying_contract", nullable = false, length = 42)
    private String verifyingContract;

    @Column(name = "typed_data_hash", nullable = false, length = 66)
    private String typedDataHash;

    @Column(name = "client_typed_data_hash", length = 66)
    private String clientTypedDataHash;

    @Column(columnDefinition = "TEXT")
    private String signature;

    @Column(name = "signature_hash", length = 66)
    private String signatureHash;

    @Column(nullable = false, length = 66)
    private String nonce;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentSignatureStatus status;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    public DocumentSignature(
            String documentId,
            Long userId,
            Long chainId,
            String verifyingContract,
            String typedDataHash,
            String nonce,
            LocalDateTime deadline
    ) {
        this.documentId = documentId;
        this.userId = userId;
        this.chainId = chainId;
        this.verifyingContract = verifyingContract;
        this.typedDataHash = typedDataHash;
        this.nonce = nonce;
        this.deadline = deadline;
        this.status = DocumentSignatureStatus.REQUESTED;
    }

    public void markSigned(String walletAddress, String signature, String signatureHash, String clientTypedDataHash) {
        this.walletAddress = walletAddress;
        this.signature = signature;
        this.signatureHash = signatureHash;
        this.clientTypedDataHash = clientTypedDataHash;
        this.status = DocumentSignatureStatus.SIGNED;
        this.signedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = DocumentSignatureStatus.EXPIRED;
    }

    public void markRejected() {
        this.status = DocumentSignatureStatus.REJECTED;
    }
}
