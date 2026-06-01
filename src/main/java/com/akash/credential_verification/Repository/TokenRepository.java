package com.akash.credential_verification.Repository;


import com.akash.credential_verification.Model.AccessToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenRepository extends MongoRepository<AccessToken, String> {}
