package com.kworkerharmony.backend.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "Internal server error"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "Invalid input value"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "Authentication is required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_403", "Access denied"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "Resource not found"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON_409", "Resource already exists"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "Invalid token"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "Refresh token not found"),
    TOKEN_TYPE_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "Token type mismatch");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
