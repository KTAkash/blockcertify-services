package com.akash.credential_verification.Repository;

import com.akash.credential_verification.Model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
    Optional<FileMetadata> findFirstByCid(String cid);
    void deleteByCid(String cid);
}
