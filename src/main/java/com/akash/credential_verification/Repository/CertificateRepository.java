package com.akash.credential_verification.Repository;

import com.akash.credential_verification.Model.Certificate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CertificateRepository extends MongoRepository<Certificate, String> {
    List<Certificate> findByStudentId(String studentId);
}
