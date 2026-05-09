package com.kworkerharmony.backend.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document.blockchain")
public record DocumentBlockchainProperties(
        long chainId,
        String contractAddress,
        String domainName,
        String domainVersion,
        long signatureTtlSeconds
) {

    public DocumentBlockchainProperties {
        if (chainId == 0) {
            chainId = 11155111L;
        }
        if (contractAddress == null || contractAddress.isBlank()) {
            contractAddress = "0x0000000000000000000000000000000000000000";
        }
        if (domainName == null || domainName.isBlank()) {
            domainName = "KWorkerHarmonyDocument";
        }
        if (domainVersion == null || domainVersion.isBlank()) {
            domainVersion = "1";
        }
        if (signatureTtlSeconds == 0) {
            signatureTtlSeconds = 900L;
        }
    }
}
