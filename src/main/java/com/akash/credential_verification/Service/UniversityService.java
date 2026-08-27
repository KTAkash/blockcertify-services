package com.akash.credential_verification.Service;

import com.akash.credential_verification.Dto.RegisterUniversityRequest;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService aesService;
    private final FabricCertificateService fabricService;


    public University register(RegisterUniversityRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new IllegalArgumentException("Field 'username' is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Field 'password' is required");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Field 'name' is required");
        }
        if (req.getMspId() == null || req.getMspId().isBlank()) {
            throw new IllegalArgumentException("Field 'mspId' is required");
        }
        if (req.getPrivateKey() == null || req.getPrivateKey().isBlank()) {
            throw new IllegalArgumentException("Field 'privateKey' is required");
        }
        if (req.getCertPem() == null || req.getCertPem().isBlank()) {
            throw new IllegalArgumentException("Field 'certPem' is required");
        }
        if (repo.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        University uni = University.builder()
                .name(req.getName())
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .mspId(req.getMspId())
                .certPem(req.getCertPem())
                .encryptedPrivateKey(aesService.encrypt(req.getPrivateKey()))
                .peerEndpoint(req.getPeerEndpoint())
                .peerTlsCertPem(req.getPeerTlsCertPem())
                .peerHostnameOverride(req.getPeerHostnameOverride())
                .build();
        return repo.save(uni);
    }

    // DEACTIVATE — soft disable, org slot freed
    public University deactivate(String universityId) {
        University uni = repo.findById(universityId)
                .orElseThrow(() -> new IllegalStateException("University not found"));
        uni.setActive(false);
        University saved = repo.save(uni);
        fabricService.evictChannelCache(universityId); // ← clear connection
        return saved;
    }

    // REACTIVATE — bring back online
    public University reactivate(String universityId) {
        University uni = repo.findById(universityId)
                .orElseThrow(() -> new IllegalStateException("University not found"));
        uni.setActive(true);
        University saved = repo.save(uni);
        fabricService.evictChannelCache(universityId);
        return saved;
    }

    // REASSIGN ORG — move university to a different org
// Only allowed if target org is not already taken
    public University reassignOrg(String universityId, String newMspId) {
        University uni = repo.findById(universityId)
                .orElseThrow(() -> new IllegalStateException("University not found"));

        // Check target org is not already taken by another active university
        boolean orgTaken = repo.findAll().stream()
                .anyMatch(u -> u.getMspId().equals(newMspId)
                        && !u.getId().equals(universityId)
                        && u.isActive());

        if (orgTaken) {
            throw new IllegalStateException("Org " + newMspId + " is already assigned to another university");
        }

        uni.setMspId(newMspId);
        // Update peer details to match new org
        uni.setPeerEndpoint(resolvePeerEndpoint(newMspId));
        uni.setPeerHostnameOverride(resolveHostname(newMspId));
        uni.setPeerTlsCertPem(resolveTlsCert(newMspId));

        // Evict cached channel — must rebuild with new org identity
        University saved = repo.save(uni);
        fabricService.evictChannelCache(universityId); // ← force rebuild with new org
        return saved;
    }

    // HARD DELETE — remove from MongoDB entirely
    public void delete(String universityId) {
        if (!repo.existsById(universityId)) {
            throw new IllegalStateException("University not found");
        }
        fabricService.evictChannelCache(universityId); // ← clear before delete
        repo.deleteById(universityId);
    }

    // Get all universities
    public java.util.List<University> getAll() {
        return repo.findAll();
    }

    // Helper methods to resolve peer details
    private String resolvePeerEndpoint(String mspId) {
        // Example: Org1MSP → 188.166.207.81:7051, Org2MSP → 188.166.207.81:9051
        if ("Org1MSP".equals(mspId)) {
            return "188.166.207.81:7051";
        } else if ("Org2MSP".equals(mspId)) {
            return "188.166.207.81:9051";
        }
        return "";
    }

    private String resolveHostname(String mspId) {
        if ("Org1MSP".equals(mspId)) {
            return "peer0.org1.example.com";
        } else if ("Org2MSP".equals(mspId)) {
            return "peer0.org2.example.com";
        }
        return "";
    }

    private String resolveTlsCert(String mspId) {
        // In real implementation, load from config or request
        return "";
    }
}