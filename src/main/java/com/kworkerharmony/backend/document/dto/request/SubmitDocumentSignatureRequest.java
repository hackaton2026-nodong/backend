package com.kworkerharmony.backend.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitDocumentSignatureRequest(
        @NotBlank(message = "Wallet address is required")
        String walletAddress,

        @NotNull(message = "Chain id is required")
        Long chainId,

        @NotBlank(message = "Signature is required")
        String signature,

        String typedDataHash,

        @NotBlank(message = "Nonce is required")
        String nonce
) {
}
