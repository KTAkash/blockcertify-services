package com.akash.credential_verification.Model;

import com.akash.credential_verification.Constants.CertificateStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "certificates")
@Data
@Builder
public class Certificate {
    @Id
    private String id;
    private String studentId;
    private String certificateTitle;
    private String cid; // IPFS CID
    private String hash; // SHA-256
    private String issuedBy;
    private CertificateStatus status;
    @CreatedDate
    private Instant issuedAt;
}
