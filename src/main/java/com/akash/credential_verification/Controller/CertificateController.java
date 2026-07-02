package com.akash.credential_verification.Controller;


import com.akash.credential_verification.Dto.CreateCertificateRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Model.UniversityPrincipal;
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

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final FabricCertificateService fabricCertificateService;
    private final UniversityRepository universityRepository;

    // ─── MongoDB ────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateCertificateRequest request, @AuthenticationPrincipal UniversityPrincipal principal) {
        University university = universityRepository.findById(principal.universityId()).orElseThrow();
        return ResponseEntity.ok(certificateService.save(request, university));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return certificateService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Blockchain ─────────────────────────────────────────────────────────

    @PostMapping("/blockchain")
    public ResponseEntity<?> createOnBlockchain(@RequestBody @Valid CreateCertificateRequest request, @AuthenticationPrincipal UniversityPrincipal principal) {
        try {
            String certificateId = fabricCertificateService.createCertificate(
                    principal.universityId(),
                    request.getCertificateId(),
                    request.getStudentId(),
                    request.getCid(),
                    request.getHash(),
                    principal.universityName(),
                    request.getStatus().name(),
                    java.time.Instant.now().toString()
            );
            return ResponseEntity.ok(Map.of("certificateId", certificateId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockchain/{id}")
    public ResponseEntity<?> getFromBlockchain(@PathVariable String id, @AuthenticationPrincipal UniversityPrincipal principal) {
        try {
            return ResponseEntity.ok(fabricCertificateService.getCertificate(principal.universityId(), id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockchain")
    public ResponseEntity<?> getAllFromBlockchain(@AuthenticationPrincipal UniversityPrincipal principal) {
        try {
            return ResponseEntity.ok(fabricCertificateService.getAllCertificates(principal.universityId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/blockchain/{id}/status")
    public ResponseEntity<?> updateBlockchainStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UniversityPrincipal principal
    ) {
        try {
            String status = request.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
            }
            fabricCertificateService.updateCertificateStatus(principal.universityId(), id, status);
            return ResponseEntity.ok(Map.of("certificateId", id, "status", status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}