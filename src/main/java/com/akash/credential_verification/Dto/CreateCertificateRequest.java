package com.akash.credential_verification.Dto;

import com.akash.credential_verification.Constants.CertificateStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCertificateRequest {

    @NotBlank(message = "Certificate ID is required")
    @JsonProperty("certificateId")
    private String certificateId;

    @NotBlank(message = "Student ID is required")
    @JsonProperty("studentId")
    private String studentId;

    @NotBlank(message = "IPFS CID is required")
    @JsonProperty("cid")
    private String cid;

    @NotBlank(message = "Hash value is required")
    @JsonProperty("hash")
    private String hash;

    @NotNull(message = "Status is required")   // @NotBlank doesn't work on enums
    @JsonProperty("status")
    private CertificateStatus status;
}