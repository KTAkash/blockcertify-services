package com.akash.credential_verification.Controller;


import com.akash.credential_verification.Constants.CertificateStatus;
import com.akash.credential_verification.Dto.BlockchainCertificateRequest;
import com.akash.credential_verification.Dto.CreateCertificateRequest;
import com.akash.credential_verification.Dto.UpdateBlockchainStatusRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Model.UniversityPrincipal;
import com.akash.credential_verification.Model.UserPrincipal;
import com.akash.credential_verification.Repository.UniversityRepository;
import com.akash.credential_verification.Service.CertificateService;
import com.akash.credential_verification.Service.FabricCertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final FabricCertificateService fabricCertificateService;
    private final UniversityRepository universityRepository;

    // ─── MongoDB ────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateCertificateRequest request, @AuthenticationPrincipal Object principal) {
        try {
            University university = getEffectiveUniversity(null, principal);
            return ResponseEntity.ok(certificateService.save(request, university));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return certificateService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(certificateService.getByStudentId(studentId));
    }

    // ─── Blockchain ─────────────────────────────────────────────────────────

    @PostMapping("/blockchain")
    public ResponseEntity<?> createOnBlockchain(@RequestBody @Valid BlockchainCertificateRequest request, @AuthenticationPrincipal Object principal) {
        try {
            University university = getEffectiveUniversity(null, principal);
            String certificateId = request.getCertificateId();
            if (certificateId == null || certificateId.isBlank()) {
                certificateId = UUID.randomUUID().toString();
            }
            String resultId = fabricCertificateService.createCertificate(
                    university.getId(),
                    certificateId,
                    request.getStudentId(),
                    request.getCid(),
                    request.getHash(),
                    university.getName(),
                    request.getStatus().name(),
                    java.time.Instant.now().toString()
            );
            return ResponseEntity.ok(Map.of("certificateId", resultId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockchain/{id}")
    public ResponseEntity<?> getFromBlockchain(
            @PathVariable String id,
            @RequestParam(required = false) String universityId,
            @AuthenticationPrincipal Object principal
    ) {
        try {
            // For read operations, use any active university's channel since world state is shared
            University university = getAnyActiveUniversity(universityId, principal);
            return ResponseEntity.ok(fabricCertificateService.getCertificate(university.getId(), id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockchain")
    public ResponseEntity<?> getAllFromBlockchain(
            @RequestParam(required = false) String universityId,
            @AuthenticationPrincipal Object principal
    ) {
        try {
            // For read operations, use any active university's channel since world state is shared
            University university = getAnyActiveUniversity(universityId, principal);
            return ResponseEntity.ok(fabricCertificateService.getAllCertificates(university.getId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/blockchain/{id}/status")
    public ResponseEntity<?> updateBlockchainStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateBlockchainStatusRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        try {
            // Use any active university's channel since world state is shared
            University university = getAnyActiveUniversity(null, principal);
            String statusStr = request.getStatus().name();

            // 1. Update Blockchain
            fabricCertificateService.updateCertificateStatus(university.getId(), id, statusStr);

            // 2. Sync MongoDB (if record exists)
            certificateService.updateStatus(id, request.getStatus());

            return ResponseEntity.ok(Map.of("certificateId", id, "status", statusStr));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private University getEffectiveUniversity(String requestedUniversityId, Object principal) {
        // 1. Try to get from authenticated principal
        if (principal instanceof UniversityPrincipal uniPrincipal) {
            return universityRepository.findById(uniPrincipal.universityId())
                    .orElseThrow(() -> new IllegalArgumentException("University not found: " + uniPrincipal.universityId()));
        }

        // 2. Try to get from requested ID (for Super Admin)
        if (requestedUniversityId != null && !requestedUniversityId.isBlank()) {
            return universityRepository.findById(requestedUniversityId)
                    .orElseThrow(() -> new IllegalArgumentException("University not found: " + requestedUniversityId));
        }

        // 3. Fallback for development: pick the first available university
        return universityRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No universities found. Please register a university first."));
    }

    // New method for read operations - more flexible fallback to any active university
    private University getAnyActiveUniversity(String requestedUniversityId, Object principal) {
        // 1. Try to get from authenticated principal
        if (principal instanceof UniversityPrincipal uniPrincipal) {
            return universityRepository.findById(uniPrincipal.universityId())
                    .orElseThrow(() -> new IllegalArgumentException("University not found: " + uniPrincipal.universityId()));
        }

        // 2. Try to get from requested ID (for Super Admin)
        if (requestedUniversityId != null && !requestedUniversityId.isBlank()) {
            return universityRepository.findById(requestedUniversityId)
                    .orElseThrow(() -> new IllegalArgumentException("University not found: " + requestedUniversityId));
        }

        // 3. For read operations, fallback to ANY active university since world state is shared
        return universityRepository.findAll().stream()
                .filter(University::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active universities found. Please register and activate a university first."));
    }
}