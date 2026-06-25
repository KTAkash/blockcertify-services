package com.akash.credential_verification.Controller;

import com.akash.credential_verification.Dto.RegisterUniversityRequest;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/universities")
@RequiredArgsConstructor
public class SuperAdminController {

    private final UniversityService universityService;

    @PostMapping
    public ResponseEntity<?> register(@RequestBody RegisterUniversityRequest request) {
        try {
            University uni = universityService.register(request);
            return ResponseEntity.ok(Map.of(
                    "universityId", uni.getId(),
                    "username", uni.getUsername(),
                    "message", "University registered. Distribute username + password securely."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
