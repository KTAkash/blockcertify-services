package com.akash.credential_verification.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "universities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class University {
    @Id
    private String id;
    private String name;
    private String username;
    private String passwordHash;
    private String mspId;
    private String certPem;
    private String encryptedPrivateKey;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
