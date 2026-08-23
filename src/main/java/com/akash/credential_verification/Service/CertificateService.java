package com.akash.credential_verification.Service;


import com.akash.credential_verification.Constants.CertificateStatus;
import com.akash.credential_verification.Dto.CreateCertificateRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository repo;

    public Certificate save(CreateCertificateRequest request, University university) {
        String certificateId = UUID.randomUUID().toString();
        Certificate certificate = Certificate.builder()
                .id(certificateId)
                .studentId(request.getStudentId())
                .certificateTitle(request.getCertificateTitle())
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

    public List<Certificate> getByStudentId(String studentId) {
        return repo.findByStudentId(studentId);
    }

    public void updateStatus(String id, CertificateStatus status) {
        repo.findById(id).ifPresent(cert -> {
            cert.setStatus(status);
            repo.save(cert);
        });
    }
}