package com.akash.credential_verification.Controller;


import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Service.CertificateService;
import com.akash.credential_verification.Service.FabricCertificateService;
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

    private final CertificateService service;
    private final FabricCertificateService fabricCertificateService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Certificate cert) {
        return ResponseEntity.ok(service.save(cert));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> issueFromFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("studentId") String studentId,
            @RequestPart("issuedBy") String issuedBy
    ) {
        try {
            return ResponseEntity.ok(service.issueFromFile(file, studentId, issuedBy));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to issue certificate"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return service.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/blockchain")
    public ResponseEntity<?> createOnBlockchain(@RequestBody Certificate cert) {
        try {
            if (cert.getId() == null || cert.getId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "id is required"));
            }

            String certificateId = fabricCertificateService.createCertificate(
                    cert.getId(),
                    cert.getStudentId(),
                    cert.getCid(),
                    cert.getHash(),
                    cert.getIssuedBy(),
                    "VALID",
                    Instant.now().toString()
            );

            return ResponseEntity.ok(Map.of("certificateId", certificateId));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/blockchain/{id}")
    public ResponseEntity<?> getFromBlockchain(@PathVariable String id) {
        try {
            return ResponseEntity.ok(fabricCertificateService.getCertificate(id));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/blockchain")
    public ResponseEntity<?> getAllFromBlockchain() {
        try {
            return ResponseEntity.ok(fabricCertificateService.getAllCertificates());
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
        }
    }

    @PatchMapping("/blockchain/{id}/status")
    public ResponseEntity<?> updateBlockchainStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        try {
            fabricCertificateService.updateCertificateStatus(id, request.get("status"));
            return ResponseEntity.ok(Map.of("certificateId", id, "status", request.get("status")));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
        }
    }
}
