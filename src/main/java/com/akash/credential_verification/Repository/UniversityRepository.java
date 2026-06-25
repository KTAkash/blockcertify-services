package com.akash.credential_verification.Repository;

import com.akash.credential_verification.Model.University;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UniversityRepository extends MongoRepository<University, String> {
    Optional<University> findByUsername(String username);
}
