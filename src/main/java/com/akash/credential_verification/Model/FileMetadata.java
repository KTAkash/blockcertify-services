package com.akash.credential_verification.Model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "file_metadata")
@Data
public class FileMetadata {
    @Id
    private String id;

    @Indexed
    private String cid;

    private String fileName;
    private String contentType;
    private long size;
    private String hash;
    private String gatewayUrl;
    private Instant uploadedAt;
}
