package com.akash.credential_verification.DTO;

public record FileUploadResponse(
        String cid,
        String fileName,
        String contentType,
        long size,
        String hash,
        String gatewayUrl
) {
}
