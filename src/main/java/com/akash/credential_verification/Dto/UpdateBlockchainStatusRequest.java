package com.akash.credential_verification.Dto;

import com.akash.credential_verification.Constants.CertificateStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBlockchainStatusRequest {

    @JsonProperty("universityId")
    private String universityId;

    @NotNull(message = "Status is required")
    @JsonProperty("status")
    private CertificateStatus status;
}
