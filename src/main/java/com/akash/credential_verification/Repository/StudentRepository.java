package com.akash.credential_verification.Repository;

import com.akash.credential_verification.Model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByIndexNo(String indexNo);
}

