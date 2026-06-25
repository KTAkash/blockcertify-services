package com.akash.credential_verification.Dto;

import lombok.Data;

@Data
public class RegisterUniversityRequest {
    private String name;
    private String username;
    private String password;
    private String mspId;
    private String certPem;
    private String privateKey;
}
