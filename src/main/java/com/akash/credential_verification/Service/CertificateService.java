package com.akash.credential_verification.Service;


import com.akash.credential_verification.Dto.CreateCertificateRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository repo;

    public Certificate save(CreateCertificateRequest request, University university) {
        Certificate certificate = Certificate.builder()
                .id(request.getCertificateId())
                .studentId(request.getStudentId())
                .CertificateTitle(request.getCertificateTitle())
                .cid(request.getCid())
                .hash(request.getHash())
                .issuedBy(university.getName())
                .status(request.getStatus())
                .build();
        return repo.save(certificate);
    }

    public Optional<Certificate> getById(String id) {
        return repo.findById(id);
    }
}