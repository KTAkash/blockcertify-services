package com.akash.credential_verification.Dto;

public record FileUploadResponse(
        String cid,
        String fileName,
        String contentType,
        long size,
        String hash,
        String gatewayUrl
) {
}
