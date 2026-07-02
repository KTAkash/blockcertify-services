package com.akash.credential_verification.Controller;

import com.akash.credential_verification.Dto.RegisterUniversityRequest;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/universities")
@RequiredArgsConstructor
public class SuperAdminController {

    private final UniversityService universityService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterUniversityRequest req) {
        University uni = universityService.register(req);
        // Never return private key or passwordHash in response
        Map<String, Object> response = new HashMap<>();
        response.put("id", uni.getId());
        response.put("name", uni.getName());
        response.put("mspId", uni.getMspId());
        response.put("peerEndpoint", uni.getPeerEndpoint());
        response.put("createdAt", uni.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    // Deactivate — org slot freed, login blocked
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable String id) {
        University uni = universityService.deactivate(id);
        return ResponseEntity.ok(Map.of(
                "id", uni.getId(),
                "name", uni.getName(),
                "active", uni.isActive(),
                "message", "University deactivated. Org slot " + uni.getMspId() + " is now available."
        ));
    }

    // Reactivate
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable String id) {
        University uni = universityService.reactivate(id);
        return ResponseEntity.ok(Map.of(
                "id", uni.getId(),
                "name", uni.getName(),
                "active", uni.isActive(),
                "message", "University reactivated successfully."
        ));
    }

    // Reassign to different org
    @PatchMapping("/{id}/reassign-org")
    public ResponseEntity<?> reassignOrg(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String newMspId = body.get("mspId");
        if (newMspId == null || newMspId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "mspId is required"));
        }
        University uni = universityService.reassignOrg(id, newMspId);
        return ResponseEntity.ok(Map.of(
                "id", uni.getId(),
                "name", uni.getName(),
                "mspId", uni.getMspId(),
                "message", "University reassigned to " + newMspId
        ));
    }

    // Hard delete
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        universityService.delete(id);
        return ResponseEntity.ok(Map.of(
                "message", "University deleted. Note: certificates issued by this university remain on the blockchain ledger."
        ));
    }

    // Get all universities (for admin dashboard)
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                universityService.getAll().stream()
                        .map(uni -> Map.of(
                                "id", uni.getId(),
                                "name", uni.getName(),
                                "mspId", uni.getMspId(),
                                "peerEndpoint", uni.getPeerEndpoint(),
                                "active", uni.isActive()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
