package com.akash.credential_verification.Service;


import com.akash.credential_verification.DTO.FileUploadResponse;
import com.akash.credential_verification.Dto.CreateCertificateRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository repo;

    public Certificate save(CreateCertificateRequest request) {
        Certificate certificate = Certificate.builder()
                .id(request.getCertificateId())
                .studentId(request.getStudentId())
                .cid(request.getCid())
                .hash(request.getHash())
                .issuedBy(request.getIssuedBy())
                .status(request.getStatus())
                .issuedAt(request.getIssuedAt())
                .build();
        return repo.save(certificate);
    }

    public Optional<Certificate> getById(String id) {
        return repo.findById(id);
    }
}