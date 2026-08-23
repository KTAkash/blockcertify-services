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

@Document(collection = "student_auths")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAuth {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private String indexNo;
    private String mobileNo;
    private String gender;
    @Builder.Default
    private UserRole role = UserRole.STUDENT;
    @CreatedDate
    private Instant createdAt;
}
