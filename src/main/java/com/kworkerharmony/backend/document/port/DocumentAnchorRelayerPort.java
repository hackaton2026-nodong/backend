package com.kworkerharmony.backend.document.port;

public interface DocumentAnchorRelayerPort {

    AnchorReceipt anchor(AnchorCommand command);

    record AnchorCommand(
            String documentHash,
            String caseIdHash,
            String signer,
            String typedDataHash,
            String signature,
            String nonce,
            long deadlineEpochSeconds,
            long chainId,
            String contractAddress,
            String anchorId
    ) {
    }

    record AnchorReceipt(
            String txHash,
            Long blockNumber
    ) {
    }
}
