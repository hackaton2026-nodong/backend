package com.kworkerharmony.backend.document.dto.response;

import com.kworkerharmony.backend.document.DocumentAnchor;
import java.time.LocalDateTime;

public record DocumentAnchorResponse(
        String anchorId,
        String documentId,
        String status,
        String contractAddress,
        Long chainId,
        String txHash,
        Long blockNumber,
        LocalDateTime anchoredAt
) {

    public static DocumentAnchorResponse from(DocumentAnchor anchor) {
        return new DocumentAnchorResponse(
                anchor.getAnchorId(),
                anchor.getDocumentId(),
                switch (anchor.getStatus()) {
                    case PENDING -> "ANCHOR_PENDING";
                    case ANCHORED -> "ANCHORED_ON_CHAIN";
                    case FAILED -> "ANCHOR_FAILED";
                },
                anchor.getContractAddress(),
                anchor.getChainId(),
                anchor.getTxHash(),
                anchor.getBlockNumber(),
                anchor.getAnchoredAt()
        );
    }
}
