package com.kworkerharmony.backend.document.support;

import com.kworkerharmony.backend.global.exception.CustomException;
import com.kworkerharmony.backend.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class DocumentCrypto {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private DocumentCrypto() {
    }

    public static String randomBytes32Hex() {
        byte[] nonce = new byte[32];
        SECURE_RANDOM.nextBytes(nonce);
        return "0x" + HexFormat.of().formatHex(nonce);
    }

    public static String bytes32HexFromSha256(String value) {
        return "0x" + sha256Hex(value);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "SHA-256 is not available");
        }
    }

    public static String ensureBytes32Hex(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Hash value is required");
        }
        return value.startsWith("0x") ? value : "0x" + value;
    }
}
