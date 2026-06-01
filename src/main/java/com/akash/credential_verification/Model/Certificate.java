package com.akash.credential_verification.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "certificates")
@Data
public class Certificate {
    @Id
    private String id;
    private String studentId;
    private String cid; // IPFS CID
    private String hash; // SHA-256
    private String issuedBy;
}
