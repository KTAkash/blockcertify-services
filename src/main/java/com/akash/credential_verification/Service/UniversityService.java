package com.akash.credential_verification.Service;

import com.akash.credential_verification.Dto.RegisterUniversityRequest;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesService;

    public University register(RegisterUniversityRequest req) {
        if (repo.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        University uni = University.builder()
                .name(req.getName())
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .mspId(req.getMspId())
                .certPem(req.getCertPem())
                .encryptedPrivateKey(aesService.encrypt(req.getPrivateKey()))
                .build();
        return repo.save(uni);
    }
}