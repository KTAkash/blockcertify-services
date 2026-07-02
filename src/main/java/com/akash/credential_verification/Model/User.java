package com.akash.credential_verification.Model;

import com.akash.credential_verification.Constants.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;

    private String username;

    private String passwordHash;

    private UserRole role; // SUPER_ADMIN, STUDENT

    @CreatedDate
    private Instant createdAt;
}
