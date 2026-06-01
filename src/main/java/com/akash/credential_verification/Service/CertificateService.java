package com.akash.credential_verification.Service;


import com.akash.credential_verification.DTO.FileUploadResponse;
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
    private final FileService fileService;

    public Certificate save(Certificate cert) {
        return repo.save(cert);
    }

    public Optional<Certificate> getById(String id) {
        return repo.findById(id);
    }

    public Certificate issueFromFile(MultipartFile file, String studentId, String issuedBy) throws IOException {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("studentId is required");
        }
        if (issuedBy == null || issuedBy.isBlank()) {
            throw new IllegalArgumentException("issuedBy is required");
        }

        FileUploadResponse upload = fileService.upload(file);

        Certificate certificate = new Certificate();
        certificate.setStudentId(studentId);
        certificate.setIssuedBy(issuedBy);
        certificate.setCid(upload.cid());
        certificate.setHash(upload.hash());

        return repo.save(certificate);
    }
}
