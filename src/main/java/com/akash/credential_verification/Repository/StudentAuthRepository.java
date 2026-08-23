package com.akash.credential_verification.Repository;

import com.akash.credential_verification.Model.StudentAuth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentAuthRepository extends MongoRepository<StudentAuth, String> {
    Optional<StudentAuth> findByEmail(String email);
    Optional<StudentAuth> findByIndexNo(String indexNo);
}
