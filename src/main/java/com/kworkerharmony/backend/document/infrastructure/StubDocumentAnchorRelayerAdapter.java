package com.kworkerharmony.backend.document.infrastructure;

import com.kworkerharmony.backend.document.port.DocumentAnchorRelayerPort;
import com.kworkerharmony.backend.document.support.DocumentCrypto;
import org.springframework.stereotype.Component;

@Component
public class StubDocumentAnchorRelayerAdapter implements DocumentAnchorRelayerPort {

    @Override
    public AnchorReceipt anchor(AnchorCommand command) {
        String txHash = DocumentCrypto.bytes32HexFromSha256("tx|" + command.anchorId());
        return new AnchorReceipt(txHash, 1L);
    }
}
