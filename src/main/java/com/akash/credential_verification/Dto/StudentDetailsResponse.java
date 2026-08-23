package com.akash.credential_verification.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailsResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String indexNo;
    private String mobileNo;
    private String gender;
    private String role;
    private Instant createdAt;

    private List<CertificateInfo> certificates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateInfo {
        private String certificateId;
        private String certificateTitle;
        private String cid;
        private String hash;
        private String issuedBy;
        private String status;
        private Instant issuedAt;
    }
}
