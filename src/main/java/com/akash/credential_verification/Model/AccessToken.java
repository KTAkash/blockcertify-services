package com.akash.credential_verification.Model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tokens")
@Data
public class AccessToken {
    @Id
    private String id;
    private String certificateId;
    private String token;
    private long expiryTime;
}
