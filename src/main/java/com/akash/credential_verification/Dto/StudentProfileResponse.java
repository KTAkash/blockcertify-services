package com.akash.credential_verification.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String indexNo;
    private String mobileNo;
    private String gender;
    private String role;
    private Instant createdAt;
}
