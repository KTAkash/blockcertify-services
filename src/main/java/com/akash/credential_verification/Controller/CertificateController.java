package com.akash.credential_verification.Controller;


import com.akash.credential_verification.Dto.CreateCertificateRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Service.CertificateService;
import com.akash.credential_verification.Service.FabricCertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // ─── MongoDB ────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateCertificateRequest request) {
        return ResponseEntity.ok(certificateService.save(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return certificateService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Blockchain ─────────────────────────────────────────────────────────

    @PostMapping("/blockchain")
    public ResponseEntity<?> createOnBlockchain(@RequestBody @Valid CreateCertificateRequest request) {
        try {
            String certificateId = fabricCertificateService.createCertificate(
                    request.getCertificateId(),
                    request.getStudentId(),
                    request.getCid(),
                    request.getHash(),
                    request.getIssuedBy(),
                    request.getStatus().name(),
                    request.getIssuedAt()
            );
            return ResponseEntity.ok(Map.of("certificateId", certificateId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockchain/{id}")
    public ResponseEntity<?> getFromBlockchain(@PathVariable String id) {
        try {
            return ResponseEntity.ok(fabricCertificateService.getCertificate(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockchain")
    public ResponseEntity<?> getAllFromBlockchain() {
        try {
            return ResponseEntity.ok(fabricCertificateService.getAllCertificates());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/blockchain/{id}/status")
    public ResponseEntity<?> updateBlockchainStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        try {
            String status = request.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
            }
            fabricCertificateService.updateCertificateStatus(id, status);
            return ResponseEntity.ok(Map.of("certificateId", id, "status", status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}